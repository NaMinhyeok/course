package io.github.naminhyeok.course.core.api.controller.v1.response

import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.enums.CourseStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class CourseDetailResponse(
    val id: Long,
    val creatorId: Long,
    val title: String,
    val description: String,
    val price: BigDecimal,
    val capacity: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
    val status: CourseStatus,
) {
    companion object {
        fun from(course: Course): CourseDetailResponse =
            CourseDetailResponse(
                id = course.id,
                creatorId = course.creatorId,
                title = course.title,
                description = course.description,
                price = course.price,
                capacity = course.capacity,
                startAt = course.startAt,
                endAt = course.endAt,
                status = course.status,
            )
    }
}
