package io.github.naminhyeok.course.storage.db.core.course

import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EntityStatus
import io.github.naminhyeok.course.storage.db.CoreDbContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
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
    fun `findByStatusAndCourseStatusNotOrderByIdDesc 는 DRAFT 와 삭제된 엔티티를 제외하고 id 내림차순 Slice 를 반환한다`() {
        courseRepository.save(courseOf(status = CourseStatus.DRAFT))
        val oldest = courseRepository.save(courseOf(status = CourseStatus.OPEN))
        val middle = courseRepository.save(courseOf(status = CourseStatus.CLOSED))
        val deleted = courseRepository.save(courseOf(status = CourseStatus.OPEN)).also { it.delete() }
        val latest = courseRepository.save(courseOf(status = CourseStatus.OPEN))

        val result =
            courseRepository.findByStatusAndCourseStatusNotOrderByIdDesc(
                status = EntityStatus.ACTIVE,
                excludedStatus = CourseStatus.DRAFT,
                pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(result.content.map { it.id }).containsExactly(latest.id, middle.id)
        assertThat(result.content.map { it.id }).doesNotContain(oldest.id, deleted.id)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    fun `findByStatusAndCourseStatusOrderByIdDesc 는 상태 필터가 있으면 해당 상태만 반환한다`() {
        courseRepository.save(courseOf(status = CourseStatus.CLOSED))
        val firstOpen = courseRepository.save(courseOf(status = CourseStatus.OPEN))
        val secondOpen = courseRepository.save(courseOf(status = CourseStatus.OPEN))
        courseRepository.save(courseOf(status = CourseStatus.DRAFT))

        val result =
            courseRepository.findByStatusAndCourseStatusOrderByIdDesc(
                status = EntityStatus.ACTIVE,
                courseStatus = CourseStatus.OPEN,
                pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(result.content.map { it.id }).containsExactly(secondOpen.id, firstOpen.id)
        assertThat(result.content).allMatch { it.courseStatus == CourseStatus.OPEN }
        assertThat(result.hasNext()).isFalse()
    }
}
