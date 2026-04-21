package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.CoreDbContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional

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
    fun `findByUserIdOrderByIdDesc 는 주어진 userId 의 모든 신청을 최신순으로 반환한다`() {
        val first = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 100L))
        val second = enrollmentRepository.save(EnrollmentEntity(courseId = 2L, userId = 100L))
        enrollmentRepository.save(EnrollmentEntity(courseId = 3L, userId = 999L))

        val result = enrollmentRepository.findByUserIdOrderByIdDesc(100L)

        assertThat(result).extracting("id").containsExactly(second.id, first.id)
    }

    @Test
    fun `findByUserIdAndEnrollmentStatusOrderByIdDesc 는 주어진 userId 와 status 의 신청만 최신순으로 반환한다`() {
        val pending = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 100L))
        val confirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 2L, userId = 100L)).also { it.confirm() }
        enrollmentRepository.save(EnrollmentEntity(courseId = 3L, userId = 100L)).also { it.cancel() }
        enrollmentRepository.save(EnrollmentEntity(courseId = 4L, userId = 999L)).also { it.confirm() }

        val result =
            enrollmentRepository.findByUserIdAndEnrollmentStatusOrderByIdDesc(
                100L,
                EnrollmentStatus.CONFIRMED,
            )

        assertThat(result).extracting("id").containsExactly(confirmed.id)
        assertThat(pending.id).isNotEqualTo(confirmed.id)
    }
}
