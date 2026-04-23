package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.OffsetLimit
import io.github.naminhyeok.course.core.support.Page
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EntityStatus
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

    fun findCourses(
        status: CourseStatus?,
        offsetLimit: OffsetLimit,
    ): Page<Course> {
        val visibleCourses =
            when (status) {
                null ->
                    courseRepository.findByStatusAndCourseStatusNotOrderByIdDesc(
                        status = EntityStatus.ACTIVE,
                        excludedStatus = CourseStatus.DRAFT,
                        pageable = offsetLimit.toPageable(),
                    )
                CourseStatus.DRAFT -> return Page(emptyList(), hasNext = false)
                else ->
                    courseRepository.findByStatusAndCourseStatusOrderByIdDesc(
                        status = EntityStatus.ACTIVE,
                        courseStatus = status,
                        pageable = offsetLimit.toPageable(),
                    )
            }
        if (visibleCourses.isEmpty) return Page(emptyList(), hasNext = false)
        val seatsByCourseId =
            courseSeatsRepository
                .findByCourseIdIn(visibleCourses.content.map { it.id })
                .associateBy { it.courseId }
        return Page(
            content =
                visibleCourses.content.map { course ->
                    Course.from(course, seatsByCourseId.getValue(course.id))
                },
            hasNext = visibleCourses.hasNext(),
        )
    }

    fun findCourse(courseId: Long): Course {
        val course =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() && it.courseStatus != CourseStatus.DRAFT }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        val seats =
            courseSeatsRepository.findByCourseId(course.id)
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        return Course.from(course, seats)
    }

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
