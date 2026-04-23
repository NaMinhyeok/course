package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class EnrollmentService(
    private val courseRepository: CourseRepository,
    private val enrollmentProcessor: EnrollmentProcessor,
    private val enrollmentManager: EnrollmentManager,
    private val enrollmentReader: EnrollmentReader,
    @Value("\${enrollment.retry.max-attempts:3}") private val maxAttempts: Int,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enroll(
        user: User,
        courseId: Long,
    ): Long {
        val course =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        if (course.courseStatus != CourseStatus.OPEN) {
            throw CoreException(ErrorType.COURSE_NOT_OPEN)
        }

        repeat(maxAttempts) { attempt ->
            try {
                return enrollmentProcessor.enroll(user.id, courseId)
            } catch (e: OptimisticLockingFailureException) {
                log.warn(
                    "[ENROLLMENT] seats 낙관 락 충돌, 재시도 attempt={} courseId={} userId={}",
                    attempt + 1,
                    courseId,
                    user.id,
                )
            }
        }
        log.error(
            "[ENROLLMENT] 재시도 소진 courseId={} userId={} maxAttempts={}",
            courseId,
            user.id,
            maxAttempts,
        )
        throw CoreException(ErrorType.ENROLLMENT_CONFLICT)
    }

    fun confirm(
        user: User,
        enrollmentId: Long,
    ) {
        enrollmentManager.confirm(user, enrollmentId)
    }

    fun cancel(
        user: User,
        enrollmentId: Long,
    ) {
        try {
            enrollmentProcessor.cancel(user, enrollmentId)
        } catch (e: OptimisticLockingFailureException) {
            log.warn(
                "[ENROLLMENT] cancel 중 seats 낙관 락 충돌 enrollmentId={}",
                enrollmentId,
            )
            throw CoreException(ErrorType.ENROLLMENT_CONFLICT)
        }
    }

    fun getEnrollments(
        user: User,
        status: EnrollmentStatus?,
    ): List<Enrollment> = enrollmentReader.getEnrollments(user.id, status)

    fun getConfirmedCourseEnrollments(
        user: User,
        courseId: Long,
    ): List<Enrollment> {
        val course =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        if (course.creatorId != user.id) {
            throw CoreException(ErrorType.ACCESS_DENIED)
        }
        return enrollmentReader.getConfirmedEnrollmentsByCourse(courseId)
    }
}
