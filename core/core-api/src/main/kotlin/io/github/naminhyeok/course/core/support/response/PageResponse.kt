package io.github.naminhyeok.course.core.support.response

data class PageResponse<T>(
    val content: List<T>,
    val hasNext: Boolean,
)
