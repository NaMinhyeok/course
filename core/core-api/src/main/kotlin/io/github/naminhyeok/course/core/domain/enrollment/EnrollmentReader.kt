package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.core.support.OffsetLimit
import io.github.naminhyeok.course.core.support.Page
import io.github.naminhyeok.course.enums.EntityStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class EnrollmentReader(
    private val enrollmentRepository: EnrollmentRepository,
    private val courseRepository: CourseRepository,
    private val courseSeatsRepository: CourseSeatsRepository,
) {
    fun getEnrollments(
        userId: Long,
        status: EnrollmentStatus?,
        offsetLimit: OffsetLimit,
    ): Page<Enrollment> {
        val entities =
            when (status) {
                null ->
                    enrollmentRepository.findByUserIdAndStatusOrderByIdDesc(
                        userId = userId,
                        status = EntityStatus.ACTIVE,
                        pageable = offsetLimit.toPageable(),
                    )
                else ->
                    enrollmentRepository.findByUserIdAndStatusAndEnrollmentStatusOrderByIdDesc(
                        userId = userId,
                        status = EntityStatus.ACTIVE,
                        enrollmentStatus = status,
                        pageable = offsetLimit.toPageable(),
                    )
            }
        if (entities.isEmpty) return Page(emptyList(), hasNext = false)

        val courseIds = entities.content.map { it.courseId }.distinct()
        val courses = courseRepository.findAllById(courseIds)
        val seatsByCourseId = courseSeatsRepository.findByCourseIdIn(courseIds).associateBy { it.courseId }
        val courseMap =
            courses
                .filter { seatsByCourseId.containsKey(it.id) }
                .associate { course ->
                    course.id to Course.from(course, seatsByCourseId[course.id]!!)
                }

        return Page(
            content =
                entities.content
                    .filter { courseMap.containsKey(it.courseId) }
                    .map { entity ->
                        Enrollment(
                            id = entity.id,
                            userId = entity.userId,
                            course = courseMap[entity.courseId]!!,
                            status = entity.enrollmentStatus,
                            appliedAt = entity.createdAt,
                        )
                    },
            hasNext = entities.hasNext(),
        )
    }

    fun getConfirmedEnrollmentsByCourse(courseId: Long): List<Enrollment> {
        val enrollments =
            enrollmentRepository.findByCourseIdAndEnrollmentStatusOrderByIdDesc(
                courseId = courseId,
                enrollmentStatus = EnrollmentStatus.CONFIRMED,
            )
        if (enrollments.isEmpty()) return emptyList()

        val course =
            courseRepository.findByIdOrNull(courseId)
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val seats =
            courseSeatsRepository.findByCourseId(courseId)
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val mappedCourse = Course.from(course, seats)

        return enrollments.map { entity ->
            Enrollment(
                id = entity.id,
                userId = entity.userId,
                course = mappedCourse,
                status = entity.enrollmentStatus,
                appliedAt = entity.createdAt,
            )
        }
    }
}
