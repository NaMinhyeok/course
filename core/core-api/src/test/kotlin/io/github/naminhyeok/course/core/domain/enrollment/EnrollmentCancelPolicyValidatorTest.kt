package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentEntity
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class EnrollmentCancelPolicyValidatorTest {
    private val zoneId = ZoneId.of("Asia/Seoul")
    private val now = LocalDateTime.of(2026, 4, 30, 10, 0)
    private val validator = EnrollmentCancelPolicyValidator(Clock.fixed(now.atZone(zoneId).toInstant(), zoneId))
    private val user = User(id = 200L)

    @Test
    fun `confirmedAt 에 7일을 더한 시각이 now 와 같으면 아직 취소할 수 있다`() {
        val enrollment = confirmedEnrollment(confirmedAt = now.minusDays(7))

        assertThatCode { validator.validate(user, enrollment) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `confirmedAt 에 7일을 더한 시각이 now 보다 1마이크로초 빠르면 만료된다`() {
        val enrollment = confirmedEnrollment(confirmedAt = now.minusDays(7).minusNanos(1_000))

        assertThatThrownBy { validator.validate(user, enrollment) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ENROLLMENT_CANCEL_EXPIRED)
    }

    @Test
    fun `confirmedAt 이 없으면 INVALID_REQUEST 예외를 던진다`() {
        val enrollment = confirmedEnrollment(confirmedAt = now.minusDays(1))
        EnrollmentEntity::class.java.getDeclaredField("confirmedAt").apply {
            isAccessible = true
            set(enrollment, null)
        }

        assertThatThrownBy { validator.validate(user, enrollment) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST)
    }

    private fun confirmedEnrollment(confirmedAt: LocalDateTime): EnrollmentEntity =
        EnrollmentEntity(
            courseId = 1L,
            userId = user.id,
            enrollmentStatus = EnrollmentStatus.CONFIRMED,
            confirmedAt = confirmedAt,
        )
}
