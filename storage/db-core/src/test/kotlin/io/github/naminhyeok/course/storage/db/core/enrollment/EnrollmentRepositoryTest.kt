package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EntityStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.CoreDbContextTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.transaction.annotation.Transactional

@Transactional
class EnrollmentRepositoryTest(
    private val enrollmentRepository: EnrollmentRepository,
) : CoreDbContextTest() {
    @Test
    fun `사용자 신청 조회는 활성 신청만 최신순으로 반환한다`() {
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
    fun `사용자 신청 조회는 상태 조건이 있으면 해당 상태만 반환한다`() {
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
    fun `강의별 확정 신청 조회는 확정된 신청만 최신순으로 반환한다`() {
        val firstConfirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 100L)).also { it.confirm() }
        val deletedConfirmed =
            enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 101L)).also {
                it.confirm()
                it.delete()
            }
        val secondConfirmed = enrollmentRepository.save(EnrollmentEntity(courseId = 1L, userId = 102L)).also { it.confirm() }
        enrollmentRepository.save(EnrollmentEntity(courseId = 2L, userId = 103L)).also { it.confirm() }

        val result =
            enrollmentRepository.findByCourseIdAndStatusAndEnrollmentStatusOrderByIdDesc(
                courseId = 1L,
                status = EntityStatus.ACTIVE,
                enrollmentStatus = EnrollmentStatus.CONFIRMED,
            )

        assertThat(result).extracting("id").containsExactly(secondConfirmed.id, firstConfirmed.id)
        assertThat(result).allMatch { it.courseId == 1L && it.enrollmentStatus == EnrollmentStatus.CONFIRMED }
        assertThat(result).extracting("id").doesNotContain(deletedConfirmed.id)
    }
}
