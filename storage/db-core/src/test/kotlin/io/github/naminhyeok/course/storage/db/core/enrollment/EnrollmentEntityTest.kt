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
    fun `대기 중인 신청이 확정되면 확정 시각이 기록된다`() {
        val enrollment = enrollmentOf(EnrollmentStatus.PENDING)

        enrollment.confirm()

        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CONFIRMED)
        assertThat(enrollment.confirmedAt).isNotNull()
    }

    @Test
    fun `대기 중인 신청은 확정 시각을 미리 가질 수 없다`() {
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
    fun `확정되었거나 취소된 신청은 확정 시각이 필요하다`() {
        assertThatThrownBy {
            EnrollmentEntity(
                courseId = 1L,
                userId = 100L,
                enrollmentStatus = EnrollmentStatus.CONFIRMED,
                confirmedAt = null,
            )
        }.isInstanceOf(IllegalStateException::class.java)

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
    fun `대기 중인 신청만 확정할 수 있다`() {
        assertThatThrownBy { enrollmentOf(EnrollmentStatus.CONFIRMED).confirm() }
            .isInstanceOf(IllegalStateException::class.java)
        assertThatThrownBy { enrollmentOf(EnrollmentStatus.CANCELLED).confirm() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `확정된 신청만 취소할 수 있다`() {
        assertThatThrownBy { enrollmentOf(EnrollmentStatus.PENDING).cancel() }
            .isInstanceOf(IllegalStateException::class.java)

        val enrollment = enrollmentOf(EnrollmentStatus.CONFIRMED)

        enrollment.cancel()

        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CANCELLED)
    }

    @Test
    fun `이미 취소된 신청은 다시 취소할 수 없다`() {
        assertThatThrownBy { enrollmentOf(EnrollmentStatus.CANCELLED).cancel() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
