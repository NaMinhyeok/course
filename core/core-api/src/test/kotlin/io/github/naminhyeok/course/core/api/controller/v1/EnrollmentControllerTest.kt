package io.github.naminhyeok.course.core.api.controller.v1

import com.ninjasquad.springmockk.MockkBean
import io.github.naminhyeok.course.core.api.config.WebConfig
import io.github.naminhyeok.course.core.api.controller.ApiControllerAdvice
import io.github.naminhyeok.course.core.domain.course.Course
import io.github.naminhyeok.course.core.domain.course.CourseSeats
import io.github.naminhyeok.course.core.domain.enrollment.Enrollment
import io.github.naminhyeok.course.core.domain.enrollment.EnrollmentService
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
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(EnrollmentController::class)
@Import(WebConfig::class, ApiControllerAdvice::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class EnrollmentControllerTest(
    private val mockMvc: MockMvc,
) {
    @MockkBean
    private lateinit var enrollmentService: EnrollmentService

    @Test
    fun `POST api v1 enrollments 는 생성된 enrollmentId 를 반환한다`() {
        every { enrollmentService.enroll(any(), 10L) } returns 42L

        mockMvc
            .perform(
                post("/api/v1/enrollments")
                    .header("X-User-Id", "200")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"courseId": 10}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.enrollmentId").value(42))

        verify { enrollmentService.enroll(any(), 10L) }
    }

    @Test
    fun `POST api v1 enrollments 는 X-User-Id 헤더가 없으면 400 을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/enrollments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"courseId": 10}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH api v1 enrollments id status CONFIRMED 는 confirm 을 호출한다`() {
        every { enrollmentService.confirm(any(), 7L) } returns Unit

        mockMvc
            .perform(
                patch("/api/v1/enrollments/7/status")
                    .header("X-User-Id", "200")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"CONFIRMED"}"""),
            ).andExpect(status().isOk)

        verify { enrollmentService.confirm(any(), 7L) }
    }

    @Test
    fun `PATCH api v1 enrollments id status PENDING 은 400 을 반환한다`() {
        mockMvc
            .perform(
                patch("/api/v1/enrollments/7/status")
                    .header("X-User-Id", "200")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"PENDING"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH api v1 enrollments id status CANCELLED 는 cancel 을 호출한다`() {
        every { enrollmentService.cancel(any(), 7L) } returns Unit

        mockMvc
            .perform(
                patch("/api/v1/enrollments/7/status")
                    .header("X-User-Id", "200")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"status":"CANCELLED"}"""),
            ).andExpect(status().isOk)

        verify { enrollmentService.cancel(any(), 7L) }
    }

    private fun sampleEnrollment(
        id: Long = 1L,
        courseId: Long = 10L,
        status: EnrollmentStatus = EnrollmentStatus.PENDING,
    ): Enrollment {
        val baseStart = LocalDateTime.of(2026, 5, 1, 0, 0)
        return Enrollment(
            id = id,
            userId = 200L,
            course =
                Course(
                    id = courseId,
                    creatorId = 100L,
                    title = "강의",
                    description = "설명",
                    price = BigDecimal("10000"),
                    startAt = baseStart,
                    endAt = baseStart.plusDays(7),
                    status = CourseStatus.OPEN,
                    seats = CourseSeats(capacity = 10, reservedCount = 1),
                ),
            status = status,
            appliedAt = LocalDateTime.of(2026, 4, 20, 10, 0),
        )
    }

    @Test
    fun `GET api v1 enrollments 는 현재 사용자의 신청 목록을 반환한다`() {
        every { enrollmentService.getEnrollments(any(), null) } returns
            listOf(sampleEnrollment(id = 42L, courseId = 10L))

        mockMvc
            .perform(
                get("/api/v1/enrollments")
                    .header("X-User-Id", "200"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].enrollmentId").value(42))
            .andExpect(jsonPath("$.data[0].course.id").value(10))
            .andExpect(jsonPath("$.data[0].course.title").value("강의"))
            .andExpect(jsonPath("$.data[0].status").value("PENDING"))
            .andExpect(jsonPath("$.data[0].appliedAt").exists())
            .andExpect(jsonPath("$.data[0].course.price").value(10000))
            .andExpect(jsonPath("$.data[0].course.capacity").value(10))
            .andExpect(jsonPath("$.data[0].course.status").value("OPEN"))
            .andExpect(jsonPath("$.data[0].course.startAt").exists())
            .andExpect(jsonPath("$.data[0].course.endAt").exists())
    }

    @Test
    fun `GET api v1 enrollments 는 status 필터를 서비스로 전달한다`() {
        every { enrollmentService.getEnrollments(any(), EnrollmentStatus.CONFIRMED) } returns
            listOf(sampleEnrollment(status = EnrollmentStatus.CONFIRMED))

        mockMvc
            .perform(
                get("/api/v1/enrollments")
                    .header("X-User-Id", "200")
                    .param("status", "CONFIRMED"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"))

        verify { enrollmentService.getEnrollments(any(), EnrollmentStatus.CONFIRMED) }
    }

    @Test
    fun `GET api v1 enrollments 는 status 파라미터 없이도 동작한다`() {
        every { enrollmentService.getEnrollments(any(), null) } returns emptyList()

        mockMvc
            .perform(
                get("/api/v1/enrollments")
                    .header("X-User-Id", "200"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data").isArray)

        verify { enrollmentService.getEnrollments(any(), null) }
    }

    @Test
    fun `GET api v1 enrollments 에 잘못된 status 쿼리 파라미터가 오면 400 을 반환한다`() {
        mockMvc
            .perform(
                get("/api/v1/enrollments")
                    .header("X-User-Id", "200")
                    .param("status", "BANANA"),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `GET api v1 enrollments 는 X-User-Id 헤더가 없으면 400 을 반환한다`() {
        mockMvc
            .perform(get("/api/v1/enrollments"))
            .andExpect(status().isBadRequest)
    }
}
