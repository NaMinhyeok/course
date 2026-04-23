package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice

interface CourseRepository : JpaRepository<CourseEntity, Long> {
    @Query(
        """
        select c
        from CourseEntity c
        where c.status = io.github.naminhyeok.course.enums.EntityStatus.ACTIVE
          and c.courseStatus <> io.github.naminhyeok.course.enums.CourseStatus.DRAFT
          and (:courseStatus is null or c.courseStatus = :courseStatus)
        """,
    )
    fun findVisibleCourses(
        @Param("courseStatus") courseStatus: CourseStatus?,
        pageable: Pageable,
    ): Slice<CourseEntity>
}
