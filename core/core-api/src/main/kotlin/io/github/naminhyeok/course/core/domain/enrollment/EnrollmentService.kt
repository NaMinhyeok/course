package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class EnrollmentService(
    private val courseRepository: CourseRepository,
    private val enrollmentProcessor: EnrollmentProcessor,
    private val enrollmentManager: EnrollmentManager,
) {
    fun enroll(
        user: User,
        courseId: Long,
    ): Long {
        val course =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        check(course.courseStatus == CourseStatus.OPEN) {
            "신청 가능한 강의 상태가 아닙니다: ${course.courseStatus}"
        }
        return enrollmentProcessor.enroll(user.id, courseId)
    }

    fun confirm(
        user: User,
        enrollmentId: Long,
    ) {
        enrollmentManager.confirm(user, enrollmentId)
    }
}
