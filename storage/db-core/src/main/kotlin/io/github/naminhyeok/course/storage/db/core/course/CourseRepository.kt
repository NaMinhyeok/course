package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EntityStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<CourseEntity, Long> {
    fun findVisibleCourses(pageable: Pageable): Slice<CourseEntity> =
        findByStatusAndCourseStatusNotOrderByIdDesc(
            status = EntityStatus.ACTIVE,
            excludedStatus = CourseStatus.DRAFT,
            pageable = pageable,
        )

    fun findCoursesByStatus(
        courseStatus: CourseStatus,
        pageable: Pageable,
    ): Slice<CourseEntity> =
        findByStatusAndCourseStatusOrderByIdDesc(
            status = EntityStatus.ACTIVE,
            courseStatus = courseStatus,
            pageable = pageable,
        )

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
