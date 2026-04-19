package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CourseService(
    private val courseRepository: CourseRepository,
) {
    fun createCourse(
        user: User,
        content: CourseContent,
    ): Long {
        val saved =
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
        return saved.id
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
        val courses =
            if (status != null) {
                courseRepository.findByCourseStatus(status)
            } else {
                courseRepository.findAll()
            }
        return courses
            .filter { it.isActive() }
            .filter { it.courseStatus != CourseStatus.DRAFT }
            .map { Course.from(it) }
    }

    fun findCourse(courseId: Long): Course {
        val course =
            courseRepository
                .findByIdOrNull(courseId)
                ?.takeIf { it.isActive() && it.courseStatus != CourseStatus.DRAFT }
                ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
        return Course.from(course)
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
