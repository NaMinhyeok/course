package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EntityStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository

interface EnrollmentRepository : JpaRepository<EnrollmentEntity, Long> {
    fun findByCourseIdAndEnrollmentStatusOrderByIdDesc(
        courseId: Long,
        enrollmentStatus: EnrollmentStatus,
    ): List<EnrollmentEntity>

    fun findByUserIdAndStatusOrderByIdDesc(
        userId: Long,
        status: EntityStatus,
        pageable: Pageable,
    ): Slice<EnrollmentEntity>

    fun findByUserIdAndStatusAndEnrollmentStatusOrderByIdDesc(
        userId: Long,
        status: EntityStatus,
        enrollmentStatus: EnrollmentStatus,
        pageable: Pageable,
    ): Slice<EnrollmentEntity>
}
