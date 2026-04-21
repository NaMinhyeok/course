package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentEntity
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class EnrollmentProcessor(
    private val courseSeatsRepository: CourseSeatsRepository,
    private val enrollmentRepository: EnrollmentRepository,
) {
    @Transactional
    fun enroll(
        userId: Long,
        courseId: Long,
    ): Long {
        val seats =
            courseSeatsRepository.findByCourseId(courseId)
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        seats.reserve()
        val enrollment =
            enrollmentRepository.save(
                EnrollmentEntity(courseId = courseId, userId = userId),
            )
        return enrollment.id
    }

    @Transactional
    fun cancel(
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
        enrollment.cancel()
        val seats =
            courseSeatsRepository.findByCourseId(enrollment.courseId)
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        seats.release()
    }
}
