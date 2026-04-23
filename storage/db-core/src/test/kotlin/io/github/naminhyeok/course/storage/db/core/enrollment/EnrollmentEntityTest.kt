package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EnrollmentStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class EnrollmentEntityTest {
    private fun enrollmentOf(status: EnrollmentStatus = EnrollmentStatus.PENDING): EnrollmentEntity =
        EnrollmentEntity(
            courseId = 1L,
            userId = 100L,
            enrollmentStatus = status,
            confirmedAt =
                when (status) {
                    EnrollmentStatus.CONFIRMED, EnrollmentStatus.CANCELLED -> LocalDateTime.of(2026, 4, 23, 10, 0)
                    else -> null
                },
        )

    @Test
    fun `PENDING 상태에서 confirm 을 호출하면 confirmedAt 이 기록된다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.PENDING)

        enrollment.confirm()

        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CONFIRMED)
        assertThat(enrollment.confirmedAt).isNotNull()
    }

    @Test
    fun `PENDING 상태에서 confirmedAt 이 있으면 생성할 수 없다`() {
        assertThatThrownBy {
            EnrollmentEntity(
                courseId = 1L,
                userId = 100L,
                enrollmentStatus = EnrollmentStatus.PENDING,
                confirmedAt = LocalDateTime.of(2026, 4, 23, 10, 0),
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CONFIRMED 상태에서 confirmedAt 이 없으면 생성할 수 없다`() {
        assertThatThrownBy {
            EnrollmentEntity(
                courseId = 1L,
                userId = 100L,
                enrollmentStatus = EnrollmentStatus.CONFIRMED,
                confirmedAt = null,
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CANCELLED 상태에서 confirmedAt 이 없으면 생성할 수 없다`() {
        assertThatThrownBy {
            EnrollmentEntity(
                courseId = 1L,
                userId = 100L,
                enrollmentStatus = EnrollmentStatus.CANCELLED,
                confirmedAt = null,
            )
        }.isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CONFIRMED 상태에서 confirm 을 호출하면 IllegalStateException 이 발생한다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.CONFIRMED)

        assertThatThrownBy { enrollment.confirm() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CANCELLED 상태에서 confirm 을 호출하면 IllegalStateException 이 발생한다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.CANCELLED)

        assertThatThrownBy { enrollment.confirm() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `PENDING 상태에서는 cancel 할 수 없다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.PENDING)

        assertThatThrownBy { enrollment.cancel() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `CONFIRMED 상태에서 cancel 을 호출하면 CANCELLED 로 전이된다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.CONFIRMED)

        enrollment.cancel()

        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CANCELLED)
    }

    @Test
    fun `CANCELLED 상태에서 cancel 을 재호출하면 IllegalStateException 이 발생한다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.CANCELLED)

        assertThatThrownBy { enrollment.cancel() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
