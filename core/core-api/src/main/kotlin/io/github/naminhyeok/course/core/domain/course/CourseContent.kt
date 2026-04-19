package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import java.math.BigDecimal
import java.time.LocalDateTime

data class CourseContent(
    val title: String,
    val description: String,
    val price: BigDecimal,
    val capacity: Int,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime,
) {
    init {
        if (capacity < 1) throw CoreException(ErrorType.INVALID_REQUEST)
        if (!endAt.isAfter(startAt)) throw CoreException(ErrorType.INVALID_REQUEST)
    }
}
