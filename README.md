# 수강 신청 시스템

강사는 강의를 등록 할 수 있고, 수강생은 수강 신청 할 수 있는 수강 신청 시스템 입니다.

## 프로젝트 개요

크리에이터가 강의를 등록하고 모집 상태를 바꾸고, 수강생이 강의를 신청한 뒤 확정하거나 취소할 수 있는 흐름을 중심으로 구현했습니다.

아래 흐름을 우선순위로 두고 구현했습니다.

- 강의 등록과 상태 전이 (`DRAFT -> OPEN -> CLOSED`)
- 강의 목록 조회와 상세 조회
- 수강 신청, 결제 확정, 수강 취소
- 내 수강 신청 목록 조회
- 정원 초과 방지
- 마지막 좌석 경쟁 상황에 대한 동시성 제어

선택 요구사항 가운데서도 아래 항목은 함께 반영했습니다.

- 결제 확정 후 7일 이내 수강 취소 제한
- 크리에이터 전용 확정 수강생 목록 조회
- 목록 조회 API 페이지네이션

## 기술 스택

- Kotlin 1.9
- Spring Boot 3.x
- Spring Web MVC
- Spring Data JPA
- H2 Database (in-memory, MySQL mode)
- JUnit 5
- AssertJ
- Awaitility
- Gradle Kotlin DSL

## 실행 방법

### 요구 환경

- JDK 24

### 로컬 실행

```bash
./gradlew bootRun
```

- 애플리케이션 기본 주소는 `http://localhost:8080`입니다.
- 헬스 체크 엔드포인트는 `GET /health`입니다.
- DB는 H2 in-memory로 실행되며, 애플리케이션 시작 시 스키마가 함께 생성됩니다.

## API 목록 및 예시

대표 요청 흐름은 [docs/api.http](./docs/api.http)에 정리했습니다.

| Method | Path | 설명 |
|---|---|---|
| POST | `/api/v1/courses` | 강의 등록 |
| PATCH | `/api/v1/courses/{courseId}/status` | 강의 상태 변경 |
| GET | `/api/v1/courses` | 강의 목록 조회 |
| GET | `/api/v1/courses/{courseId}` | 강의 상세 조회 |
| GET | `/api/v1/courses/{courseId}/enrollments` | 크리에이터 전용 확정 수강생 목록 조회 |
| POST | `/api/v1/enrollments` | 수강 신청 |
| PATCH | `/api/v1/enrollments/{enrollmentId}/status` | 결제 확정 또는 수강 취소 |
| GET | `/api/v1/enrollments` | 내 수강 신청 목록 조회 |

목록 조회 API는 공통적으로 `offset`, `limit`를 받고, `status`는 필요할 때만 넘기도록 했습니다.

- `GET /api/v1/courses?status=OPEN&offset=0&limit=10`
- `GET /api/v1/enrollments?status=CONFIRMED&offset=0&limit=10`

### 예시 1. 강의 등록

요청:

```http
POST /api/v1/courses
X-User-Id: 100
Content-Type: application/json

{
  "title": "Spring Boot 입문",
  "description": "처음 배우는 Spring",
  "price": 50000,
  "capacity": 30,
  "startAt": "2026-05-01T00:00:00",
  "endAt": "2026-05-31T23:59:59"
}
```

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "courseId": 1
  },
  "error": null
}
```

### 예시 2. 강의 목록 조회

요청:

```http
GET /api/v1/courses?status=OPEN&offset=0&limit=2
```

응답:

```json
{
  "result": "SUCCESS",
  "data": {
    "content": [
      {
        "id": 3,
        "title": "Spring Boot 입문",
        "price": 50000,
        "capacity": 30,
        "startAt": "2026-05-01T00:00:00",
        "endAt": "2026-05-31T23:59:59",
        "status": "OPEN"
      }
    ],
    "hasNext": false
  },
  "error": null
}
```

### 예시 3. 취소 가능 기간 초과

응답:

```json
{
  "result": "ERROR",
  "data": null,
  "error": {
    "code": "E400",
    "message": "수강 취소 가능 기간이 지났습니다.",
    "data": null
  }
}
```

자주 보게 되는 비즈니스 오류는 아래와 같습니다.

- `COURSE_NOT_OPEN`: 신청 가능한 상태의 강의가 아닌 경우
- `CAPACITY_EXCEEDED`: 정원이 모두 소진된 경우
- `ENROLLMENT_CONFLICT`: 동시성 충돌이 재시도 횟수 안에 해소되지 않은 경우
- `ENROLLMENT_CANCEL_EXPIRED`: 결제 확정 후 7일이 지난 경우

## 데이터 모델 설명

### 관계 요약

- `course` 1 : 1 `course_seats`
- `course` 1 : N `enrollment`

### 테이블 설명

| 테이블 | 역할 |
|---|---|
| `course` | 강의 기본 정보와 공개 상태를 관리합니다. |
| `course_seats` | 강의별 정원과 현재 예약 수를 관리합니다. |
| `enrollment` | 사용자별 수강 신청 이력과 상태를 관리합니다. |

### 모델링 의도

- `course`는 제목, 설명, 가격, 기간, 공개 상태처럼 강의 자체의 정보를 담습니다.
- `course_seats`는 정원과 예약 수를 따로 관리해서 좌석 경쟁이 생기는 지점을 분리했습니다.
- `enrollment`는 사용자와 강의 사이에서 신청이 어떤 상태로 진행되고 있는지 기록합니다.
- 실제 사용자 및 강사 테이블은 과제 범위 밖이라 `creatorId`, `userId`만 참조 값으로 두었습니다.

참고 문서:

- DDL: [docs/schema.sql](./docs/schema.sql)
- 설계 근거: [docs/decisions.md](./docs/decisions.md)

## 요구사항 해석 및 가정

- 인증과 인가는 과제 허용 범위에 맞춰 `X-User-Id` 헤더 기반으로 단순화했습니다.
- 결제는 외부 결제 시스템을 붙이지 않고 상태 전이(`PENDING -> CONFIRMED`)로 처리했습니다.
- `DRAFT` 강의는 아직 공개되지 않은 초안으로 보고, 공개 목록과 상세 조회에서는 제외했습니다.
- 수강 취소는 `CONFIRMED` 상태에서만 가능하고, `confirmedAt` 기준 7일 이내만 허용하도록 해석했습니다.
- 크리에이터 전용 수강생 목록은 해당 강의의 `CONFIRMED` 신청만 보여주도록 제한했습니다.
- 목록 조회 API는 `offset`, `limit` 인터페이스를 사용합니다.

## 설계 결정과 이유

1. 좌석 수 변경은 경합이 몰리는 영역이라 `course_seats` 테이블로 따로 분리했습니다.
2. `course_seats`에 `@Version`을 두고 낙관 락과 재시도를 조합해 마지막 좌석 경쟁 상황에서도 정원 초과가 나지 않게 했습니다.
3. 수강 신청 흐름은 `EnrollmentService`, `EnrollmentProcessor`, `EnrollmentManager`, `EnrollmentReader`로 나눠 재시도와 트랜잭션 경계가 섞이지 않도록 했습니다.
4. JPA Entity와 API 응답용 도메인 모델을 분리해 영속성 세부사항이 서비스와 컨트롤러 바깥으로 직접 새지 않도록 했습니다.

상세한 배경은 [docs/decisions.md](./docs/decisions.md)에 정리했습니다.

## 테스트 실행 방법

```bash
./gradlew test
./gradlew :core:core-api:test --tests '*EnrollmentConcurrencyTest'
./gradlew :core:core-api:test --tests '*EnrollmentRetryExhaustionTest'
```

- 전체 테스트: `./gradlew test`
- 동시성 핵심 시나리오: `EnrollmentConcurrencyTest`
- 재시도 소진 시나리오: `EnrollmentRetryExhaustionTest`

동시성 제어 방식과 테스트 근거는 [docs/concurrency.md](./docs/concurrency.md)에 정리했습니다.

## 미구현 / 제약사항

### 미구현

- 대기열(waitlist) 기능

### 제약사항

- 실제 결제 시스템 연동은 포함하지 않았으며, 결제 완료는 상태 전이로 대체했습니다.
- 인증과 인가는 `X-User-Id` 헤더 기반으로 단순화했습니다.
- 공개 API 기준으로 `DRAFT` 강의는 조회 대상에서 제외했습니다.
- 동시성 검증은 단일 애플리케이션과 단일 DB 환경에서 수행했습니다.
- 페이지네이션 인터페이스는 `offset`, `limit`를 사용하지만, 내부적으로는 페이지 단위 조회로 변환해 처리합니다. 상세 규칙은 [docs/pagination.md](./docs/pagination.md)를 참고할 수 있습니다.

## AI 활용 범위

이번 과제는 AI와 페어 프로그래밍하는 방식으로 진행했습니다. 요구사항 해석, 범위 판단, 설계 결정, 최종 검토는 직접 했고, 구현과 문서 초안 정리 과정에서 AI를 보조 도구로 활용했습니다.

### 직접 수행한 내용

- 요구사항 해석과 구현 범위 결정
- 도메인 구조와 책임 분리에 대한 설계 판단
- 구현 방향과 수정 사항에 대한 의사결정
- 테스트 실행과 제출물 검토

### AI를 활용한 내용

- API, 서비스, 엔티티, 리포지토리 구현 초안 작성
- 테스트 코드 초안 작성과 보완
- README와 보조 문서 초안 정리
- 리팩터링과 문장 정리 보조

### 최종 책임

요구사항 충족 여부 판단, 코드 반영 여부 확인, 테스트 실행과 최종 제출 책임은 작성자 본인에게 있습니다.
