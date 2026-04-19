package io.github.naminhyeok.course.core.api.controller.v1.request

import io.github.naminhyeok.course.enums.CourseStatus

data class ChangeCourseStatusRequest(
    val status: CourseStatus,
)
