package io.github.naminhyeok.course.core.domain.course

data class CourseSeats(
    val capacity: Int,
    val reservedCount: Int,
) {
    fun availableCount(): Int = capacity - reservedCount

    fun isFull(): Boolean = reservedCount >= capacity
}
