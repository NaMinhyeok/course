package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CourseContentTest {
    private val baseStart = LocalDateTime.of(2026, 5, 1, 0, 0)
    private val baseEnd = baseStart.plusDays(7)

    @Test
    fun `capacity 가 1 이상이고 endAt 이 startAt 이후이면 생성된다`() {
        assertThatCode {
            CourseContent(
                title = "t",
                description = "d",
                price = BigDecimal("10000"),
                capacity = 1,
                startAt = baseStart,
                endAt = baseEnd,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `capacity 가 0 이하이면 INVALID_REQUEST 예외를 던진다`() {
        assertThatThrownBy {
            CourseContent(
                title = "t",
                description = "d",
                price = BigDecimal("10000"),
                capacity = 0,
                startAt = baseStart,
                endAt = baseEnd,
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST)
    }

    @Test
    fun `endAt 이 startAt 과 같거나 이전이면 INVALID_REQUEST 예외를 던진다`() {
        assertThatThrownBy {
            CourseContent(
                title = "t",
                description = "d",
                price = BigDecimal("10000"),
                capacity = 10,
                startAt = baseStart,
                endAt = baseStart,
            )
        }.isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST)
    }
}
