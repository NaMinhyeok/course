package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class CourseEntityTest {
    private fun courseOf(status: CourseStatus = CourseStatus.DRAFT): CourseEntity {
        val now = LocalDateTime.of(2026, 5, 1, 0, 0)
        return CourseEntity(
            creatorId = 1L,
            title = "테스트 강의",
            description = "설명",
            price = BigDecimal("10000"),
            capacity = 10,
            startAt = now,
            endAt = now.plusDays(7),
            courseStatus = status,
        )
    }

    @Test
    fun `초안 강의는 공개할 수 있다`() {
        val course = courseOf(CourseStatus.DRAFT)

        course.open()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.OPEN)
    }

    @Test
    fun `이미 공개된 강의는 다시 공개해도 상태를 유지한다`() {
        val course = courseOf(CourseStatus.OPEN)

        course.open()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.OPEN)
    }

    @Test
    fun `닫힌 강의는 다시 공개할 수 없다`() {
        val course = courseOf(CourseStatus.CLOSED)

        assertThatThrownBy { course.open() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `공개된 강의는 닫을 수 있다`() {
        val course = courseOf(CourseStatus.OPEN)

        course.close()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.CLOSED)
    }

    @Test
    fun `이미 닫힌 강의는 다시 닫아도 상태를 유지한다`() {
        val course = courseOf(CourseStatus.CLOSED)

        course.close()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.CLOSED)
    }

    @Test
    fun `초안 강의는 바로 닫을 수 없다`() {
        val course = courseOf(CourseStatus.DRAFT)

        assertThatThrownBy { course.close() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
