package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "course",
    indexes = [
        Index(name = "idx_course_creator_id", columnList = "creatorId"),
        Index(name = "idx_course_course_status", columnList = "courseStatus"),
    ],
)
class CourseEntity(
    val creatorId: Long,
    val title: String,
    @Column(length = 2000)
    val description: String,
    val price: BigDecimal,
    val capacity: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    courseStatus: CourseStatus = CourseStatus.DRAFT,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    final var courseStatus: CourseStatus = courseStatus
        private set

    fun open() {
        if (courseStatus == CourseStatus.OPEN) return
        check(courseStatus == CourseStatus.DRAFT) { "유효하지 않은 상태 전이입니다: $courseStatus -> OPEN" }
        courseStatus = CourseStatus.OPEN
    }

    fun close() {
        if (courseStatus == CourseStatus.CLOSED) return
        check(courseStatus == CourseStatus.OPEN) { "유효하지 않은 상태 전이입니다: $courseStatus -> CLOSED" }
        courseStatus = CourseStatus.CLOSED
    }
}
