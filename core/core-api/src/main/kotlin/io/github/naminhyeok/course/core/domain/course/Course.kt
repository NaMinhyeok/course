package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsEntity
import java.math.BigDecimal
import java.time.LocalDateTime

data class Course(
    val id: Long,
    val creatorId: Long,
    val title: String,
    val description: String,
    val price: BigDecimal,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: CourseStatus,
    val seats: CourseSeats,
) {
    companion object {
        fun from(
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
    }
}
