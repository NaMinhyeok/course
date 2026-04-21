package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.storage.db.core.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(
    name = "course_seats",
    indexes = [
        Index(name = "udx_course_seats_course_id", columnList = "courseId", unique = true),
    ],
)
class CourseSeatsEntity(
    val courseId: Long,
    val capacity: Int,
    reservedCount: Int = 0,
    @Version
    private var version: Long = 0,
) : BaseEntity() {
    final var reservedCount: Int = reservedCount
        private set

    fun reserve() {
        check(reservedCount < capacity) { "정원을 초과했습니다 (capacity=$capacity)" }
        reservedCount += 1
    }

    fun release() {
        check(reservedCount > 0) { "예약된 좌석이 없습니다" }
        reservedCount -= 1
    }
}
