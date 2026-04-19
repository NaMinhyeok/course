package io.github.naminhyeok.course.core.api.controller.v1.request

import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CreateCourseRequestTest {
    private val baseStart = LocalDateTime.of(2026, 5, 1, 0, 0)

    private fun req(
        title: String = "강의",
        description: String = "설명",
        price: BigDecimal = BigDecimal("10000"),
        capacity: Int = 10,
    ) = CreateCourseRequest(
        title = title,
        description = description,
        price = price,
        capacity = capacity,
        startAt = baseStart,
        endAt = baseStart.plusDays(7),
    )

    @Test
    fun `정상 입력은 CourseContent 로 변환된다`() {
        val content = req().toContent()

        assertThat(content.title).isEqualTo("강의")
        assertThat(content.capacity).isEqualTo(10)
    }

    @Test
    fun `title 이 비어있으면 INVALID_REQUEST 를 던진다`() {
        assertThatThrownBy { req(title = "").toContent() }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST)

        assertThatThrownBy { req(title = "   ").toContent() }
            .isInstanceOf(CoreException::class.java)
    }

    @Test
    fun `title 이 100 자를 초과하면 INVALID_REQUEST 를 던진다`() {
        assertThatThrownBy { req(title = "a".repeat(101)).toContent() }
            .isInstanceOf(CoreException::class.java)
    }

    @Test
    fun `description 이 비어있거나 2000 자를 초과하면 INVALID_REQUEST 를 던진다`() {
        assertThatThrownBy { req(description = "").toContent() }
            .isInstanceOf(CoreException::class.java)
        assertThatThrownBy { req(description = "a".repeat(2001)).toContent() }
            .isInstanceOf(CoreException::class.java)
    }

    @Test
    fun `price 가 음수이면 INVALID_REQUEST 를 던진다`() {
        assertThatThrownBy { req(price = BigDecimal("-1")).toContent() }
            .isInstanceOf(CoreException::class.java)
    }
}
