package io.github.naminhyeok.course.core.support

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OffsetLimitTest {
    @Test
    fun `toPageable 는 offset 을 page 시작점으로 해석한다`() {
        val pageable = OffsetLimit(offset = 15, limit = 10).toPageable()

        assertThat(pageable.pageNumber).isEqualTo(1)
        assertThat(pageable.pageSize).isEqualTo(10)
        assertThat(pageable.offset).isEqualTo(10)
    }
}
