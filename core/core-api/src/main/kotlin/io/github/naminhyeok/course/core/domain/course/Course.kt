package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.enums.CourseStatus
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
)
