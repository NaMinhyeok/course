package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestConstructor
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@Tag("context")
@SpringBootTest(properties = ["enrollment.retry.max-attempts=1"])
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class EnrollmentRetryExhaustionTest(
    private val enrollmentService: EnrollmentService,
    private val courseRepository: CourseRepository,
    private val courseSeatsRepository: CourseSeatsRepository,
    private val enrollmentRepository: EnrollmentRepository,
) {
    private val creatorId = 100L
    private val baseStart: LocalDateTime = LocalDateTime.of(2026, 5, 1, 0, 0)

    @AfterEach
    fun cleanup() {
        enrollmentRepository.deleteAll()
        courseSeatsRepository.deleteAll()
        courseRepository.deleteAll()
    }

    @Test
    fun `재시도 횟수를 1 로 제한하면 경합 상황에서 ENROLLMENT_CONFLICT 가 발생한다`() {
        val course = saveOpenCourseWithSeats(capacity = 20)
        val contenders = 10
        val latch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(contenders)
        val results = CopyOnWriteArrayList<Result<Long>>()

        repeat(contenders) { i ->
            executor.submit {
                latch.await()
                results +=
                    runCatching {
                        enrollmentService.enroll(User(id = 2_000L + i), course.id)
                    }
            }
        }
        latch.countDown()

        await atMost Duration.ofSeconds(5) until { results.size == contenders }
        executor.shutdown()

        val successCount = results.count { it.isSuccess }
        val conflictCount =
            results.count { it.exceptionOrNull()?.errorType() == ErrorType.ENROLLMENT_CONFLICT }
        val capacityExceededCount =
            results.count { it.exceptionOrNull()?.errorType() == ErrorType.CAPACITY_EXCEEDED }

        assertThat(conflictCount).isGreaterThan(0)
        assertThat(capacityExceededCount).isZero()
        assertThat(successCount + conflictCount).isEqualTo(contenders)
    }

    private fun saveOpenCourseWithSeats(capacity: Int): CourseEntity {
        val course =
            courseRepository.save(
                CourseEntity(
                    creatorId = creatorId,
                    title = "강의",
                    description = "설명",
                    price = BigDecimal("10000"),
                    capacity = capacity,
                    startAt = baseStart,
                    endAt = baseStart.plusDays(7),
                    courseStatus = CourseStatus.OPEN,
                ),
            )
        courseSeatsRepository.save(CourseSeatsEntity(courseId = course.id, capacity = capacity))
        return course
    }

    private fun Throwable?.errorType(): ErrorType? = (this as? CoreException)?.errorType
}
