package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EnrollmentStatus
import org.springframework.data.jpa.repository.JpaRepository

interface EnrollmentRepository : JpaRepository<EnrollmentEntity, Long> {
    fun findByUserIdOrderByIdDesc(userId: Long): List<EnrollmentEntity>

    fun findByUserIdAndEnrollmentStatusOrderByIdDesc(
        userId: Long,
        enrollmentStatus: EnrollmentStatus,
    ): List<EnrollmentEntity>
}
