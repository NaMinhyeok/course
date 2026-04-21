package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.storage.db.CoreDbContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

@Transactional
class CourseSeatsRepositoryTest(
    private val courseSeatsRepository: CourseSeatsRepository,
) : CoreDbContextTest() {
    @Test
    fun `findByCourseId 는 해당 courseId 의 엔티티를 반환한다`() {
        courseSeatsRepository.save(CourseSeatsEntity(courseId = 1L, capacity = 10))
        courseSeatsRepository.save(CourseSeatsEntity(courseId = 2L, capacity = 20))

        val found = courseSeatsRepository.findByCourseId(1L)

        assertThat(found).isNotNull
        assertThat(found?.courseId).isEqualTo(1L)
        assertThat(found?.capacity).isEqualTo(10)
    }

    @Test
    fun `findByCourseId 는 일치하는 courseId 가 없으면 null 을 반환한다`() {
        val found = courseSeatsRepository.findByCourseId(999L)

        assertThat(found).isNull()
    }
}
