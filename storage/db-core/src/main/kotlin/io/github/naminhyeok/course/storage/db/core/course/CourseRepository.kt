package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EntityStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<CourseEntity, Long> {
    fun findByStatusAndCourseStatusNotOrderByIdDesc(
        status: EntityStatus,
        excludedStatus: CourseStatus,
        pageable: Pageable,
    ): Slice<CourseEntity>

    fun findByStatusAndCourseStatusOrderByIdDesc(
        status: EntityStatus,
        courseStatus: CourseStatus,
        pageable: Pageable,
    ): Slice<CourseEntity>
}
