package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentEntity
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class EnrollmentCancelPolicyValidator {
    fun validate(
        user: User,
        enrollment: EnrollmentEntity,
    ) {
        if (enrollment.userId != user.id) {
            throw CoreException(ErrorType.ACCESS_DENIED)
        }
        check(enrollment.enrollmentStatus == EnrollmentStatus.CONFIRMED) {
            "CONFIRMED 상태에서만 취소할 수 있습니다: ${enrollment.enrollmentStatus}"
        }
        val confirmedAt = checkNotNull(enrollment.confirmedAt) {
            "CONFIRMED 상태의 수강 신청에는 confirmedAt 이 필요합니다"
        }
        if (confirmedAt.plusDays(CANCEL_AVAILABLE_DAYS).isBefore(LocalDateTime.now())) {
            throw CoreException(ErrorType.ENROLLMENT_CANCEL_EXPIRED)
        }
    }

    private companion object {
        const val CANCEL_AVAILABLE_DAYS = 7L
    }
}
