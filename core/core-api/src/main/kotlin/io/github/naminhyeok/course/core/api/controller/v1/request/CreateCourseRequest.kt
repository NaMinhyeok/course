package io.github.naminhyeok.course.core.api.controller.v1.request

import io.github.naminhyeok.course.core.domain.course.CourseContent
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateCourseRequest(
    val title: String,
    val description: String,
    val price: BigDecimal,
    val capacity: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
) {
    fun toContent(): CourseContent {
        if (title.isBlank()) throw CoreException(ErrorType.INVALID_REQUEST)
        if (title.length > 100) throw CoreException(ErrorType.INVALID_REQUEST)
        if (description.isBlank()) throw CoreException(ErrorType.INVALID_REQUEST)
        if (description.length > 2000) throw CoreException(ErrorType.INVALID_REQUEST)
        if (price < BigDecimal.ZERO) throw CoreException(ErrorType.INVALID_REQUEST)

        return CourseContent(
            title = title,
            description = description,
            price = price,
            capacity = capacity,
            startAt = startAt,
            endAt = endAt,
        )
    }
}
