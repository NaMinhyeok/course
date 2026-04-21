package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.enums.EnrollmentStatus
import java.time.LocalDateTime

data class Enrollment(
    val id: Long,
    val userId: Long,
    val course: Course,
    val status: EnrollmentStatus,
    val appliedAt: LocalDateTime,
)
