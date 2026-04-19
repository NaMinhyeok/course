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
    fun `DRAFT 상태에서 open 을 호출하면 OPEN 으로 전이된다`() {
        val course = courseOf(CourseStatus.DRAFT)

        course.open()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.OPEN)
    }

    @Test
    fun `OPEN 상태에서 open 을 재호출해도 OPEN 을 유지한다 - 멱등`() {
        val course = courseOf(CourseStatus.OPEN)

        course.open()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.OPEN)
    }

    @Test
    fun `CLOSED 상태에서 open 을 호출하면 IllegalStateException 예외가 발생한다`() {
        val course = courseOf(CourseStatus.CLOSED)

        assertThatThrownBy { course.open() }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `OPEN 상태에서 close 를 호출하면 CLOSED 로 전이된다`() {
        val course = courseOf(CourseStatus.OPEN)

        course.close()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.CLOSED)
    }

    @Test
    fun `CLOSED 상태에서 close 를 재호출해도 CLOSED 를 유지한다 - 멱등`() {
        val course = courseOf(CourseStatus.CLOSED)

        course.close()

        assertThat(course.courseStatus).isEqualTo(CourseStatus.CLOSED)
    }

    @Test
    fun `DRAFT 상태에서 close 를 호출하면 IllegalStateException 예외가 발생한다`() {
        val course = courseOf(CourseStatus.DRAFT)

        assertThatThrownBy { course.close() }
            .isInstanceOf(IllegalStateException::class.java)
    }
}
