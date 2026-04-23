package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.storage.db.core.course.error.CourseSeatsCapacityExceededException
import io.github.naminhyeok.course.storage.db.core.course.error.CourseSeatsNotReservedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CourseSeatsEntityTest {
    private fun seatsOf(
        capacity: Int = 2,
        reservedCount: Int = 0,
    ): CourseSeatsEntity =
        CourseSeatsEntity(
            courseId = 1L,
            capacity = capacity,
            reservedCount = reservedCount,
        )

    @Test
    fun `reserve 를 호출하면 reservedCount 가 1 증가한다`() {
        val seats = seatsOf(capacity = 2, reservedCount = 0)

        seats.reserve()

        assertThat(seats.reservedCount).isEqualTo(1)
    }

    @Test
    fun `capacity 경계까지 reserve 를 반복 호출할 수 있다`() {
        val seats = seatsOf(capacity = 2, reservedCount = 0)

        seats.reserve()
        seats.reserve()

        assertThat(seats.reservedCount).isEqualTo(2)
    }

    @Test
    fun `capacity 에 도달한 상태에서 reserve 를 호출하면 CourseSeatsCapacityExceededException 이 발생한다`() {
        val seats = seatsOf(capacity = 2, reservedCount = 2)

        assertThatThrownBy { seats.reserve() }
            .isInstanceOf(CourseSeatsCapacityExceededException::class.java)
    }

    @Test
    fun `release 를 호출하면 reservedCount 가 1 감소한다`() {
        val seats = seatsOf(capacity = 2, reservedCount = 1)

        seats.release()

        assertThat(seats.reservedCount).isEqualTo(0)
    }

    @Test
    fun `reservedCount 가 0 인 상태에서 release 를 호출하면 CourseSeatsNotReservedException 이 발생한다`() {
        val seats = seatsOf(capacity = 2, reservedCount = 0)

        assertThatThrownBy { seats.release() }
            .isInstanceOf(CourseSeatsNotReservedException::class.java)
    }
}
