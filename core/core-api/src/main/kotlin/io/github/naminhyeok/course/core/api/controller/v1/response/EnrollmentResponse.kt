package io.github.naminhyeok.course.core.api.controller.v1.response

import io.github.naminhyeok.course.core.domain.enrollment.Enrollment
import io.github.naminhyeok.course.enums.EnrollmentStatus
import java.time.LocalDateTime

data class EnrollmentResponse(
    val enrollmentId: Long,
    val course: CourseSummaryResponse,
    val status: EnrollmentStatus,
    val appliedAt: LocalDateTime,
) {
    companion object {
        fun from(enrollment: Enrollment): EnrollmentResponse =
            EnrollmentResponse(
                enrollmentId = enrollment.id,
                course = CourseSummaryResponse.from(enrollment.course),
                status = enrollment.status,
                appliedAt = enrollment.appliedAt,
            )
    }
}
