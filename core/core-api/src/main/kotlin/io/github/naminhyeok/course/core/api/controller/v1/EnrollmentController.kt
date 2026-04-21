package io.github.naminhyeok.course.core.api.controller.v1

import io.github.naminhyeok.course.core.api.controller.v1.request.ChangeEnrollmentStatusRequest
import io.github.naminhyeok.course.core.api.controller.v1.request.EnrollRequest
import io.github.naminhyeok.course.core.api.controller.v1.response.EnrollResponse
import io.github.naminhyeok.course.core.api.controller.v1.response.EnrollmentResponse
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.domain.enrollment.EnrollmentService
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.core.support.response.ApiResponse
import io.github.naminhyeok.course.enums.EnrollmentStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
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

    @PatchMapping("/api/v1/enrollments/{enrollmentId}/status")
    fun changeStatus(
        user: User,
        @PathVariable enrollmentId: Long,
        @RequestBody request: ChangeEnrollmentStatusRequest,
    ): ApiResponse<Any> {
        when (request.status) {
            EnrollmentStatus.CONFIRMED -> enrollmentService.confirm(user, enrollmentId)
            EnrollmentStatus.CANCELLED -> enrollmentService.cancel(user, enrollmentId)
            EnrollmentStatus.PENDING -> throw CoreException(ErrorType.INVALID_REQUEST)
        }
        return ApiResponse.success()
    }

    @GetMapping("/api/v1/enrollments")
    fun getEnrollments(
        user: User,
        @RequestParam(required = false) status: EnrollmentStatus?,
    ): ApiResponse<List<EnrollmentResponse>> {
        val enrollments = enrollmentService.getEnrollments(user, status)
        return ApiResponse.success(enrollments.map { EnrollmentResponse.from(it) })
    }
}
