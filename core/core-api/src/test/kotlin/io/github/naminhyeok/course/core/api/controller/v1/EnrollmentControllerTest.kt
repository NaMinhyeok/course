package io.github.naminhyeok.course.core.api.controller.v1

import com.ninjasquad.springmockk.MockkBean
import io.github.naminhyeok.course.core.api.config.WebConfig
import io.github.naminhyeok.course.core.api.controller.ApiControllerAdvice
import io.github.naminhyeok.course.core.domain.enrollment.EnrollmentService
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.TestConstructor
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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
}
