package io.github.naminhyeok.course.core.api.controller.v1

import io.github.naminhyeok.course.core.api.controller.v1.request.ChangeCourseStatusRequest
import io.github.naminhyeok.course.core.api.controller.v1.request.CreateCourseRequest
import io.github.naminhyeok.course.core.api.controller.v1.response.CourseDetailResponse
import io.github.naminhyeok.course.core.api.controller.v1.response.CourseSummaryResponse
import io.github.naminhyeok.course.core.api.controller.v1.response.CreateCourseResponse
import io.github.naminhyeok.course.core.api.controller.v1.response.EnrollmentResponse
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.domain.course.CourseService
import io.github.naminhyeok.course.core.domain.enrollment.EnrollmentService
import io.github.naminhyeok.course.core.support.OffsetLimit
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.core.support.response.ApiResponse
import io.github.naminhyeok.course.core.support.response.PageResponse
import io.github.naminhyeok.course.enums.CourseStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CourseController(
    private val courseService: CourseService,
    private val enrollmentService: EnrollmentService,
) {
    @PostMapping("/api/v1/courses")
    fun createCourse(
        user: User,
        @RequestBody request: CreateCourseRequest,
    ): ApiResponse<CreateCourseResponse> {
        val courseId = courseService.createCourse(user, request.toContent())
        return ApiResponse.success(CreateCourseResponse(courseId))
    }

    @PatchMapping("/api/v1/courses/{courseId}/status")
    fun changeCourseStatus(
        user: User,
        @PathVariable courseId: Long,
        @RequestBody request: ChangeCourseStatusRequest,
    ): ApiResponse<Any> {
        when (request.status) {
            CourseStatus.OPEN -> courseService.openCourse(user, courseId)
            CourseStatus.CLOSED -> courseService.closeCourse(user, courseId)
            CourseStatus.DRAFT -> throw CoreException(ErrorType.INVALID_REQUEST)
        }
        return ApiResponse.success()
    }

    @GetMapping("/api/v1/courses")
    fun findCourses(
        @RequestParam(required = false) status: CourseStatus?,
        @RequestParam offset: Int,
        @RequestParam limit: Int,
    ): ApiResponse<PageResponse<CourseSummaryResponse>> {
        val courses = courseService.findCourses(status, OffsetLimit(offset, limit))
        return ApiResponse.success(PageResponse(courses.content.map { CourseSummaryResponse.from(it) }, courses.hasNext))
    }

    @GetMapping("/api/v1/courses/{courseId}")
    fun findCourse(
        @PathVariable courseId: Long,
    ): ApiResponse<CourseDetailResponse> {
        val course = courseService.findCourse(courseId)
        return ApiResponse.success(CourseDetailResponse.from(course))
    }

    @GetMapping("/api/v1/courses/{courseId}/enrollments")
    fun getConfirmedCourseEnrollments(
        user: User,
        @PathVariable courseId: Long,
    ): ApiResponse<List<EnrollmentResponse>> {
        val enrollments = enrollmentService.getConfirmedCourseEnrollments(user, courseId)
        return ApiResponse.success(enrollments.map { EnrollmentResponse.from(it) })
    }
}
