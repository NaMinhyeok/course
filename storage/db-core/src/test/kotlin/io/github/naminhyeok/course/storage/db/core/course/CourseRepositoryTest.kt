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
    fun `강의 목록 조회는 초안과 삭제된 강의를 제외한다`() {
        val oldest = courseRepository.save(courseOf(status = CourseStatus.OPEN))
        val middle = courseRepository.save(courseOf(status = CourseStatus.CLOSED))
        val deleted = courseRepository.save(courseOf(status = CourseStatus.OPEN)).also { it.delete() }
        val draft = courseRepository.save(courseOf(status = CourseStatus.DRAFT))
        val latest = courseRepository.save(courseOf(status = CourseStatus.OPEN))

        val result =
            courseRepository.findByStatusAndCourseStatusNotOrderByIdDesc(
                status = EntityStatus.ACTIVE,
                excludedStatus = CourseStatus.DRAFT,
                pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(result.content.map { it.id }).containsExactly(latest.id, middle.id)
        assertThat(result.content.map { it.id }).doesNotContain(oldest.id, deleted.id, draft.id)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    fun `강의 목록 조회는 상태 조건이 있으면 해당 상태만 반환한다`() {
        courseRepository.save(courseOf(status = CourseStatus.CLOSED))
        val firstOpen = courseRepository.save(courseOf(status = CourseStatus.OPEN))
        val secondOpen = courseRepository.save(courseOf(status = CourseStatus.OPEN))
        val deletedOpen = courseRepository.save(courseOf(status = CourseStatus.OPEN)).also { it.delete() }
        courseRepository.save(courseOf(status = CourseStatus.DRAFT))

        val result =
            courseRepository.findByStatusAndCourseStatusOrderByIdDesc(
                status = EntityStatus.ACTIVE,
                courseStatus = CourseStatus.OPEN,
                pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(result.content.map { it.id }).containsExactly(secondOpen.id, firstOpen.id)
        assertThat(result.content).allMatch { it.courseStatus == CourseStatus.OPEN }
        assertThat(result.content.map { it.id }).doesNotContain(deletedOpen.id)
        assertThat(result.hasNext()).isFalse()
    }
}
