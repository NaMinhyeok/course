package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<CourseEntity, Long> {
    fun findByCourseStatus(courseStatus: CourseStatus): List<CourseEntity>
}
