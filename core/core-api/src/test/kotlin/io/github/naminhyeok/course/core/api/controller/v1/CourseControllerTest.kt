package io.github.naminhyeok.course.core.api.controller.v1

import com.ninjasquad.springmockk.MockkBean
import io.github.naminhyeok.course.core.api.config.WebConfig
import io.github.naminhyeok.course.core.api.controller.ApiControllerAdvice
import io.github.naminhyeok.course.core.api.controller.v1.request.CreateCourseRequest
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.core.domain.course.CourseSeats
import io.github.naminhyeok.course.core.domain.course.CourseService
import io.github.naminhyeok.course.core.domain.enrollment.Enrollment
import io.github.naminhyeok.course.core.domain.enrollment.EnrollmentService
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(CourseController::class)
@Import(WebConfig::class, ApiControllerAdvice::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class CourseControllerTest(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper,
) {
    @MockkBean
    private lateinit var courseService: CourseService

    @MockkBean
    private lateinit var enrollmentService: EnrollmentService

    private val baseStart = LocalDateTime.of(2026, 5, 1, 0, 0)

    private fun sampleCourse(
        id: Long = 1L,
        status: CourseStatus = CourseStatus.OPEN,
    ) = Course(
        id = id,
        creatorId = 100L,
        title = "t",
        description = "d",
        price = BigDecimal("10000"),
        startAt = baseStart,
        endAt = baseStart.plusDays(7),
        status = status,
        seats = CourseSeats(capacity = 10, reservedCount = 0),
    )

    private fun sampleEnrollment(
        id: Long = 11L,
        courseId: Long = 1L,
        status: EnrollmentStatus = EnrollmentStatus.CONFIRMED,
    ) = Enrollment(
        id = id,
        userId = 200L,
        course = sampleCourse(id = courseId),
        status = status,
        appliedAt = LocalDateTime.of(2026, 4, 20, 10, 0),
    )

    @Test
    fun `POST api v1 courses 는 생성된 courseId 를 반환한다`() {
        every { courseService.createCourse(any(), any()) } returns 42L
        val body = CreateCourseRequest("t", "d", BigDecimal("10000"), 10, baseStart, baseStart.plusDays(7))

        mockMvc
            .perform(
                post("/api/v1/courses")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.courseId").value(42))
    }

    @Test
    fun `POST api v1 courses 는 X-User-Id 헤더가 없으면 400 을 반환한다`() {
        val body = CreateCourseRequest("t", "d", BigDecimal("10000"), 10, baseStart, baseStart.plusDays(7))

        mockMvc
            .perform(
                post("/api/v1/courses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH api v1 courses id status OPEN 은 openCourse 를 호출한다`() {
        every { courseService.openCourse(any(), 1L) } returns Unit

        mockMvc
            .perform(
                patch("/api/v1/courses/1/status")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"OPEN"}"""),
            ).andExpect(status().isOk)

        verify { courseService.openCourse(any(), 1L) }
    }

    @Test
    fun `PATCH api v1 courses id status CLOSED 는 closeCourse 를 호출한다`() {
        every { courseService.closeCourse(any(), 1L) } returns Unit

        mockMvc
            .perform(
                patch("/api/v1/courses/1/status")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"CLOSED"}"""),
            ).andExpect(status().isOk)

        verify { courseService.closeCourse(any(), 1L) }
    }

    @Test
    fun `PATCH api v1 courses id status DRAFT 는 400 을 반환한다`() {
        mockMvc
            .perform(
                patch("/api/v1/courses/1/status")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"DRAFT"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET api v1 courses 는 status 필터를 서비스로 전달한다`() {
        every { courseService.findCourses(CourseStatus.OPEN) } returns listOf(sampleCourse())

        mockMvc
            .perform(get("/api/v1/courses").param("status", "OPEN"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1))
    }

    @Test
    fun `GET api v1 courses 는 status 파라미터 없이도 동작한다`() {
        every { courseService.findCourses(null) } returns listOf(sampleCourse())

        mockMvc
            .perform(get("/api/v1/courses"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1))
    }

    @Test
    fun `GET api v1 courses id 는 공개 강의를 반환한다`() {
        every { courseService.findCourse(1L) } returns sampleCourse()

        mockMvc
            .perform(get("/api/v1/courses/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.creatorId").value(100))
    }

    @Test
    fun `GET api v1 courses id 는 존재하지 않으면 400 을 반환한다`() {
        every { courseService.findCourse(99L) } throws CoreException(ErrorType.NOT_FOUND_DATA)

        mockMvc
            .perform(get("/api/v1/courses/99"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET api v1 courses id enrollments 는 생성자의 confirmed enrollment 목록을 반환한다`() {
        val user = User(100L)
        every { enrollmentService.getConfirmedCourseEnrollments(user, 1L) } returns
            listOf(sampleEnrollment(id = 11L, courseId = 1L))

        mockMvc
            .perform(
                get("/api/v1/courses/1/enrollments")
                    .header("X-User-Id", "100"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].enrollmentId").value(11))
            .andExpect(jsonPath("$.data[0].course.id").value(1))
            .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"))

        verify { enrollmentService.getConfirmedCourseEnrollments(user, 1L) }
    }

    @Test
    fun `GET api v1 courses id enrollments 는 권한이 없으면 403 을 반환한다`() {
        every { enrollmentService.getConfirmedCourseEnrollments(any(), 1L) } throws
            CoreException(ErrorType.ACCESS_DENIED)

        mockMvc
            .perform(
                get("/api/v1/courses/1/enrollments")
                    .header("X-User-Id", "200"),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET api v1 courses id enrollments 는 존재하지 않는 강의면 400 을 반환한다`() {
        every { enrollmentService.getConfirmedCourseEnrollments(any(), 99L) } throws
            CoreException(ErrorType.NOT_FOUND_DATA)

        mockMvc
            .perform(
                get("/api/v1/courses/99/enrollments")
                    .header("X-User-Id", "100"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET api v1 courses id enrollments 는 X-User-Id 헤더가 없으면 400 을 반환한다`() {
        mockMvc
            .perform(get("/api/v1/courses/1/enrollments"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH api v1 courses id status 에 잘못된 enum 값이 오면 400 을 반환한다`() {
        mockMvc
            .perform(
                patch("/api/v1/courses/1/status")
                    .header("X-User-Id", "100")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"BANANA"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET api v1 courses 에 잘못된 status 쿼리 파라미터가 오면 400 을 반환한다`() {
        mockMvc
            .perform(get("/api/v1/courses").param("status", "BANANA"))
            .andExpect(status().isBadRequest)
    }
}
