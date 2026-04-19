package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.CoreDbContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Transactional
class CourseRepositoryTest(
    private val courseRepository: CourseRepository,
) : CoreDbContextTest() {
    private fun courseOf(
        creatorId: Long = 1L,
        status: CourseStatus = CourseStatus.DRAFT,
    ): CourseEntity {
        val now = LocalDateTime.of(2026, 5, 1, 0, 0)
        return CourseEntity(
            creatorId = creatorId,
            title = "강의-$creatorId-$status",
            description = "설명",
            price = BigDecimal("10000"),
            capacity = 10,
            startAt = now,
            endAt = now.plusDays(7),
            courseStatus = status,
        )
    }

    @Test
    fun `findByCourseStatus 는 해당 상태의 엔티티만 반환한다`() {
        courseRepository.save(courseOf(status = CourseStatus.DRAFT))
        courseRepository.save(courseOf(status = CourseStatus.OPEN))
        courseRepository.save(courseOf(status = CourseStatus.OPEN))
        courseRepository.save(courseOf(status = CourseStatus.CLOSED))

        val openCourses = courseRepository.findByCourseStatus(CourseStatus.OPEN)

        assertThat(openCourses).hasSize(2)
        assertThat(openCourses).allMatch { it.courseStatus == CourseStatus.OPEN }
    }

    @Test
    fun `findAll 은 저장된 모든 엔티티를 반환한다`() {
        courseRepository.save(courseOf(status = CourseStatus.DRAFT))
        courseRepository.save(courseOf(status = CourseStatus.OPEN))
        courseRepository.save(courseOf(status = CourseStatus.CLOSED))

        val all = courseRepository.findAll()

        assertThat(all).hasSize(3)
    }
}
