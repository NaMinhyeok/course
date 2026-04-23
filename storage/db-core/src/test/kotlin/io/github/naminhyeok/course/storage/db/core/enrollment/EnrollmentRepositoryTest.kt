package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EntityStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.CoreDbContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional
class EnrollmentRepositoryTest(
    private val enrollmentRepository: EnrollmentRepository,
) : CoreDbContextTest() {
    @Test
    fun `저장된 엔티티는 기본 상태가 PENDING 이다`() {
        val saved =
            enrollmentRepository.save(
                EnrollmentEntity(courseId = 1L, userId = 100L),
            )

        val found = enrollmentRepository.findById(saved.id).orElseThrow()
        assertThat(found.enrollmentStatus).isEqualTo(EnrollmentStatus.PENDING)
        assertThat(found.courseId).isEqualTo(1L)
        assertThat(found.userId).isEqualTo(100L)
    }

    @Test
    fun `findByUserIdAndStatusOrderByIdDesc 는 사용자 신청을 id 내림차순 Slice 로 반환한다`() {
        val oldest = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 100L))
        val middle = enrollmentRepository.save(EnrollmentEntity(courseId = 2L, userId = 100L))
        enrollmentRepository.save(EnrollmentEntity(courseId = 3L, userId = 999L))
        enrollmentRepository.save(EnrollmentEntity(courseId = 4L, userId = 100L)).also { it.delete() }
        val latest = enrollmentRepository.save(EnrollmentEntity(courseId = 5L, userId = 100L))

        val result =
            enrollmentRepository.findByUserIdAndStatusOrderByIdDesc(
                userId = 100L,
                status = EntityStatus.ACTIVE,
                pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(result.content.map { it.id }).containsExactly(latest.id, middle.id)
        assertThat(result.content.map { it.id }).doesNotContain(oldest.id)
        assertThat(result.hasNext()).isTrue()
    }

    @Test
    fun `findByUserIdAndStatusAndEnrollmentStatusOrderByIdDesc 는 상태 필터가 있으면 해당 상태만 반환한다`() {
        enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 100L))
        val firstConfirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 2L, userId = 100L)).also { it.confirm() }
        val secondConfirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 3L, userId = 100L)).also { it.confirm() }
        enrollmentRepository.save(EnrollmentEntity(courseId = 3L, userId = 100L)).also {
            it.confirm()
            it.cancel()
        }
        enrollmentRepository.save(EnrollmentEntity(courseId = 4L, userId = 999L)).also { it.confirm() }

        val result =
            enrollmentRepository.findByUserIdAndStatusAndEnrollmentStatusOrderByIdDesc(
                userId = 100L,
                status = EntityStatus.ACTIVE,
                enrollmentStatus = EnrollmentStatus.CONFIRMED,
                pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")),
            )

        assertThat(result.content.map { it.id }).containsExactly(secondConfirmed.id, firstConfirmed.id)
        assertThat(result.content).allMatch { it.enrollmentStatus == EnrollmentStatus.CONFIRMED }
        assertThat(result.hasNext()).isFalse()
    }

    @Test
    fun `findByCourseIdAndEnrollmentStatusOrderByIdDesc 는 주어진 courseId 와 status 의 신청만 최신순으로 반환한다`() {
        val firstConfirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 100L)).also { it.confirm() }
        val pending = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 101L))
        val secondConfirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 102L)).also { it.confirm() }
        enrollmentRepository.save(EnrollmentEntity(courseId = 2L, userId = 103L)).also { it.confirm() }

        val result =
            enrollmentRepository.findByCourseIdAndEnrollmentStatusOrderByIdDesc(
                1L,
                EnrollmentStatus.CONFIRMED,
            )

        assertThat(result).extracting("id").containsExactly(secondConfirmed.id, firstConfirmed.id)
        assertThat(result).allMatch { it.courseId == 1L && it.enrollmentStatus == EnrollmentStatus.CONFIRMED }
        assertThat(pending.id).isNotEqualTo(secondConfirmed.id)
    }

    @Test
    fun `CONFIRMED 상태의 confirmedAt 은 저장 후 조회된다`() {
        val confirmedAt = LocalDateTime.of(2026, 4, 23, 10, 0)
        val saved =
            enrollmentRepository.save(
                EnrollmentEntity(
                    courseId = 1L,
                    userId = 100L,
                    enrollmentStatus = EnrollmentStatus.CONFIRMED,
                    confirmedAt = confirmedAt,
                ),
            )

        val found = enrollmentRepository.findById(saved.id).orElseThrow()

        assertThat(found.enrollmentStatus).isEqualTo(EnrollmentStatus.CONFIRMED)
        assertThat(found.confirmedAt).isEqualTo(confirmedAt)
    }
}
