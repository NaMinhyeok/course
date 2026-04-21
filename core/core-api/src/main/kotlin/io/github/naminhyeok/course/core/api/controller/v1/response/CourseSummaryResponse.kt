package io.github.naminhyeok.course.core.api.controller.v1.response

import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.enums.CourseStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class CourseSummaryResponse(
    val id: Long,
    val title: String,
    val price: BigDecimal,
    val capacity: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: CourseStatus,
) {
    companion object {
        fun from(course: Course): CourseSummaryResponse =
            CourseSummaryResponse(
                id = course.id,
                title = course.title,
                price = course.price,
                capacity = course.seats.capacity,
                startAt = course.startAt,
                endAt = course.endAt,
                status = course.status,
            )
    }
}
