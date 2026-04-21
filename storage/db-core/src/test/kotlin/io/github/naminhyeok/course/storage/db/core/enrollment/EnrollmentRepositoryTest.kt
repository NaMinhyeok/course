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
}
