package io.github.naminhyeok.course.core.support

data class Page<T>(
    val content: List<T>,
    val hasNext: Boolean,
)
