package io.github.naminhyeok.course.core.api.controller.v1

import io.github.naminhyeok.course.core.api.controller.v1.request.EnrollRequest
import io.github.naminhyeok.course.core.api.controller.v1.response.EnrollResponse
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.domain.enrollment.EnrollmentService
import io.github.naminhyeok.course.core.support.response.ApiResponse
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EnrollmentController(
    private val enrollmentService: EnrollmentService,
) {
    @PostMapping("/api/v1/enrollments")
    fun enroll(
        user: User,
        @RequestBody request: EnrollRequest,
    ): ApiResponse<EnrollResponse> {
        val enrollmentId = enrollmentService.enroll(user, request.courseId)
        return ApiResponse.success(EnrollResponse(enrollmentId))
    }
}
