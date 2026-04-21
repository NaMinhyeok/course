package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CourseService(
    private val courseRepository: CourseRepository,
    private val courseSeatsRepository: CourseSeatsRepository,
) {
    @Transactional
    fun createCourse(
        user: User,
        content: CourseContent,
    ): Long {
        val savedCourse =
            courseRepository.save(
                CourseEntity(
                    creatorId = user.id,
                    title = content.title,
                    description = content.description,
                    price = content.price,
                    capacity = content.capacity,
                    startAt = content.startAt,
                    endAt = content.endAt,
                ),
            )
        courseSeatsRepository.save(
            CourseSeatsEntity(
                courseId = savedCourse.id,
                capacity = content.capacity,
            ),
        )
        return savedCourse.id
    }

    @Transactional
    fun openCourse(
        user: User,
        courseId: Long,
    ) {
        requireOwnedCourse(user, courseId).open()
    }

    @Transactional
    fun closeCourse(
        user: User,
        courseId: Long,
    ) {
        requireOwnedCourse(user, courseId).close()
    }

    fun findCourses(status: CourseStatus?): List<Course> {
        val entities =
            if (status != null) {
                courseRepository.findByCourseStatus(status)
            } else {
                courseRepository.findAll()
            }
        val visible =
            entities
                .filter { it.isActive() }
                .filter { it.courseStatus != CourseStatus.DRAFT }
        if (visible.isEmpty()) return emptyList()
        val seatsMap =
            courseSeatsRepository
                .findByCourseIdIn(visible.map { it.id })
                .associateBy { it.courseId }
        return visible.map { entity -> toCourse(entity, seatsMap.getValue(entity.id)) }
    }

    fun findCourse(courseId: Long): Course {
        val entity =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() && it.courseStatus != CourseStatus.DRAFT }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val seats =
            courseSeatsRepository.findByCourseId(entity.id)
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        return toCourse(entity, seats)
    }

    private fun toCourse(
        entity: CourseEntity,
        seats: CourseSeatsEntity,
    ): Course =
        Course(
            id = entity.id,
            creatorId = entity.creatorId,
            title = entity.title,
            description = entity.description,
            price = entity.price,
            startAt = entity.startAt,
            endAt = entity.endAt,
            status = entity.courseStatus,
            seats = CourseSeats(capacity = seats.capacity, reservedCount = seats.reservedCount),
        )

    private fun requireOwnedCourse(
        user: User,
        courseId: Long,
    ): CourseEntity {
        val course =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        if (course.creatorId != user.id) {
            throw CoreException(ErrorType.ACCESS_DENIED)
        }
        return course
    }
}
