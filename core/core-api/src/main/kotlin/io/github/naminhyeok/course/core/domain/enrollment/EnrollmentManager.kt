package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EnrollmentManager(
    private val enrollmentRepository: EnrollmentRepository,
) {
    @Transactional
    fun confirm(
        user: User,
        enrollmentId: Long,
    ) {
        val enrollment =
            enrollmentRepository
                .findByIdOrNull(enrollmentId)
                ?.takeIf { it.isActive() }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        if (enrollment.userId != user.id) {
            throw CoreException(ErrorType.ACCESS_DENIED)
        }
        enrollment.confirm()
    }
}
