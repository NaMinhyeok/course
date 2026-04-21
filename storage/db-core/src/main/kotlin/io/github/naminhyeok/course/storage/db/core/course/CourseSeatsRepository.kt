package io.github.naminhyeok.course.storage.db.core.course

import org.springframework.data.jpa.repository.JpaRepository

interface CourseSeatsRepository : JpaRepository<CourseSeatsEntity, Long> {
    fun findByCourseId(courseId: Long): CourseSeatsEntity?

    fun findByCourseIdIn(courseIds: Collection<Long>): List<CourseSeatsEntity>
}
