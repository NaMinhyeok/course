package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
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
    ): List<Enrollment> {
        val entities =
            if (status == null) {
                enrollmentRepository.findByUserIdOrderByIdDesc(userId)
            } else {
                enrollmentRepository.findByUserIdAndEnrollmentStatusOrderByIdDesc(userId, status)
            }
        if (entities.isEmpty()) return emptyList()

        val courseIds = entities.map { it.courseId }.distinct()
        val courses = courseRepository.findAllById(courseIds)
        val seatsByCourseId = courseSeatsRepository.findByCourseIdIn(courseIds).associateBy { it.courseId }
        val courseMap =
            courses
                .filter { seatsByCourseId.containsKey(it.id) }
                .associate { course ->
                    course.id to Course.from(course, seatsByCourseId[course.id]!!)
                }

        return entities
            .filter { courseMap.containsKey(it.courseId) }
            .map { entity ->
                Enrollment(
                    id = entity.id,
                    userId = entity.userId,
                    course = courseMap[entity.courseId]!!,
                    status = entity.enrollmentStatus,
                    appliedAt = entity.createdAt,
                )
            }
    }
}
