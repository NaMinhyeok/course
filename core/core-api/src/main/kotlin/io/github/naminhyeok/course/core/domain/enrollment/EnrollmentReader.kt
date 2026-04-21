package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.core.domain.course.CourseSeats
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
                enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId)
            } else {
                enrollmentRepository.findByUserIdAndEnrollmentStatusOrderByCreatedAtDesc(userId, status)
            }
        if (entities.isEmpty()) return emptyList()

        val courseIds = entities.map { it.courseId }.distinct()
        val courses = courseRepository.findAllById(courseIds)
        val seatsByCourseId = courseSeatsRepository.findByCourseIdIn(courseIds).associateBy { it.courseId }
        val courseMap =
            courses.associate { course ->
                val seats = seatsByCourseId.getValue(course.id)
                course.id to
                    Course(
                        id = course.id,
                        creatorId = course.creatorId,
                        title = course.title,
                        description = course.description,
                        price = course.price,
                        startAt = course.startAt,
                        endAt = course.endAt,
                        status = course.courseStatus,
                        seats = CourseSeats(capacity = seats.capacity, reservedCount = seats.reservedCount),
                    )
            }

        return entities.map { entity ->
            Enrollment(
                id = entity.id,
                userId = entity.userId,
                course = courseMap.getValue(entity.courseId),
                status = entity.enrollmentStatus,
                appliedAt = entity.createdAt,
            )
        }
    }
}
