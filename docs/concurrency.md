# 동시성 제어 — 설계와 증빙

수강 신청에서 "정원 5인 강의에 6명이 몰리면 정확히 5명만 성공해야 한다"는 조건을 어떻게 보장했는지,
그리고 이를 테스트로 어떻게 확인했는지 정리한 문서입니다.

> 왜 낙관 락을 선택했고, 왜 비관 락을 선택하지 않았는지에 대한 배경은 [`decisions.md` §2](./decisions.md)에 정리했습니다.
> 이 문서는 그 결정이 실제 코드와 테스트에서 어떻게 드러나는지에 조금 더 집중합니다.

---

## 1. 경합 지점

수강 신청 한 건에서는 아래 작업이 한 덩어리로 안전하게 처리되어야 합니다.

1. `course_seats.reserved_count < capacity`인지 확인합니다.
2. `reserved_count += 1`을 수행합니다.
3. `enrollment` 행을 INSERT합니다. (`status = PENDING`)

2단계에서 두 스레드가 같은 `reserved_count` 값을 읽고 각각 +1을 수행하면 정원 초과가 발생할 수 있습니다.
실제로 경합이 붙는 지점은 `course_seats` 한 행입니다.

---

## 2. 제어 구조

### 낙관 락
`CourseSeatsEntity`에 `@Version` 필드를 두어 갱신 시점의 버전 일치 여부를 검증합니다.
한 트랜잭션이 `reserved_count`를 갱신하는 동안 다른 트랜잭션도 같은 행을 읽었다면, 나중에 반영되는 쪽은 버전 불일치로 실패합니다.

```kotlin
// CourseSeatsEntity.kt
@Version
private var version: Long = 0

fun reserve() {
    if (reservedCount >= capacity) {
        throw CourseSeatsCapacityExceededException(...)
    }
    reservedCount += 1
}
```

### 재시도 루프
충돌은 `OptimisticLockingFailureException`으로 드러납니다.
`EnrollmentService.enroll`은 이 예외를 잡아 최대 `enrollment.retry.max-attempts`회(기본 3회)까지 재시도합니다.

```kotlin
// EnrollmentService.kt
repeat(maxAttempts) { attempt ->
    try {
        return enrollmentProcessor.enroll(user.id, courseId)
    } catch (e: OptimisticLockingFailureException) {
        log.warn("[ENROLLMENT] seats 낙관 락 충돌, 재시도 attempt={} ...", attempt + 1, ...)
    }
}
throw CoreException(ErrorType.ENROLLMENT_CONFLICT)
```

### 트랜잭션 경계
재시도 루프는 **트랜잭션 바깥**에 둡니다.
`EnrollmentService`에는 `@Transactional`을 두지 않았고, 각 시도마다 `EnrollmentProcessor.enroll`(`@Transactional`)을 호출합니다.
재시도가 기존 트랜잭션 안에서 반복되면 같은 영속성 컨텍스트를 계속 사용하게 되어 의미가 없어지기 때문에, 이 분리가 중요합니다.

---

## 3. 실패 모드

| 시나리오 | 결과 | 응답 |
|---|---|---|
| 정원 여유 있음 | 성공 | 201 + enrollmentId |
| 정원 초과 | `CourseSeatsCapacityExceededException` → `CAPACITY_EXCEEDED` | 409 |
| 재시도 안에서 충돌 해소 | 성공 | 201 + enrollmentId |
| 재시도 소진 | `ENROLLMENT_CONFLICT` | 409 |

`CAPACITY_EXCEEDED`와 `ENROLLMENT_CONFLICT`는 같은 409라도 의미가 다릅니다.
전자는 "좌석이 이미 모두 찼다"는 뜻이고, 후자는 "경합이 심해 이번 요청은 일시적으로 실패했다"는 뜻입니다.
사용자 메시지도 이 차이를 반영하도록 구분했습니다.

---

## 4. 테스트 증빙

두 테스트를 같이 보면 낙관 락과 재시도가 어떤 역할을 하는지 더 분명하게 드러납니다.

### 4.1 `EnrollmentConcurrencyTest` — 정상 케이스
- 정원 5인 강의에 10명이 동시에 신청합니다.
- 기대 결과는 정확히 5명 성공(PENDING), 5명 `CAPACITY_EXCEEDED`, `ENROLLMENT_CONFLICT`는 0건입니다.
- 재시도는 충분히 주기 위해 `max-attempts=10`으로 설정합니다.

```kotlin
@TestPropertySource(properties = ["enrollment.retry.max-attempts=10"])
...

val successCount = results.count { it.isSuccess }
val capacityExceededCount = results.count { it.exceptionOrNull()?.errorType() == ErrorType.CAPACITY_EXCEEDED }
val enrollmentConflictCount = results.count { it.exceptionOrNull()?.errorType() == ErrorType.ENROLLMENT_CONFLICT }

assertThat(successCount).isEqualTo(5)
assertThat(capacityExceededCount).isEqualTo(5)
assertThat(enrollmentConflictCount).isZero()

val seats = courseSeatsRepository.findByCourseId(course.id)
assertThat(seats?.reservedCount).isEqualTo(5)
```

여기서 `reservedCount` 검증이 중요합니다.
성공한 5건만큼 좌석 수가 정확히 5가 되어야 하며, 이 값이 결국 정합성이 깨지지 않았다는 직접적인 근거가 됩니다.

### 4.2 `EnrollmentRetryExhaustionTest` — 대조 케이스
- 정원은 20이고, 동시에 10명이 신청합니다.
- 좌석 자체는 남아 있지만 재시도 횟수를 `max-attempts=1`로 제한합니다.
- 기대 결과는 일부 성공, 일부 `ENROLLMENT_CONFLICT`, `CAPACITY_EXCEEDED`는 0건입니다.

```kotlin
@SpringBootTest(properties = ["enrollment.retry.max-attempts=1"])
...

assertThat(conflictCount).isGreaterThan(0)
assertThat(capacityExceededCount).isZero()
assertThat(successCount + conflictCount).isEqualTo(contenders)
```

이 테스트는 재시도가 충분하지 않으면 낙관 락 충돌이 그대로 사용자에게 보일 수 있다는 점을 보여줍니다.
결국 4.1에서 `ENROLLMENT_CONFLICT`가 0건이었던 이유가 재시도 덕분이었다는 점도 함께 확인할 수 있습니다.

### 4.3 동시 출발 패턴
두 테스트 모두 아래와 같은 패턴을 사용합니다.

```kotlin
val latch = CountDownLatch(1)
val executor = Executors.newFixedThreadPool(contenders)

repeat(contenders) { i ->
    executor.submit {
        latch.await()                 // 전원 대기
        results += runCatching { enrollmentService.enroll(...) }
    }
}
latch.countDown()                     // 일제히 출발

await atMost Duration.ofSeconds(5) until { results.size == contenders }
```

`CountDownLatch`로 모든 작업이 준비된 뒤 동시에 출발시키고,
`Awaitility`로 결과가 모두 모일 때까지 기다립니다.
고정된 `Thread.sleep` 대신 조건식(`results.size == contenders`)을 기준으로 기다리기 때문에 타이밍 의존성이 적습니다.

---

## 5. 실행 방법

```bash
./gradlew :core:core-api:test --tests '*EnrollmentConcurrencyTest'
./gradlew :core:core-api:test --tests '*EnrollmentRetryExhaustionTest'
```

---

## 6. 한계

- 테스트는 단일 JVM 내 멀티스레드 경합을 기준으로 합니다.
  실제 다중 인스턴스 환경의 경합과 완전히 동일하다고 볼 수는 없지만, 핵심 제어는 DB 레벨의 `@Version`에 의존하므로 기본 방향은 같습니다.
- 재시도 정책에는 횟수 제한만 있고 백오프는 두지 않았습니다.
  현재 범위에서는 즉시 재시도가 더 단순하다고 보았고, 백오프가 필요할 정도의 경합이라면 락 전략 자체를 다시 검토해야 한다고 판단했습니다.
