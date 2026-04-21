package io.github.naminhyeok.course.core.api.controller.v1.request

import io.github.naminhyeok.course.enums.EnrollmentStatus

data class ChangeEnrollmentStatusRequest(
    val status: EnrollmentStatus,
)
