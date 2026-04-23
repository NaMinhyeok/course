package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EnrollmentStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EnrollmentRepository : JpaRepository<EnrollmentEntity, Long> {
    fun findByCourseIdAndEnrollmentStatusOrderByIdDesc(
        courseId: Long,
        enrollmentStatus: EnrollmentStatus,
    ): List<EnrollmentEntity>

    @Query(
        """
        select e
        from EnrollmentEntity e
        where e.status = io.github.naminhyeok.course.enums.EntityStatus.ACTIVE
          and e.userId = :userId
          and (:enrollmentStatus is null or e.enrollmentStatus = :enrollmentStatus)
        """,
    )
    fun findActiveByUserIdAndEnrollmentStatus(
        @Param("userId") userId: Long,
        @Param("enrollmentStatus") enrollmentStatus: EnrollmentStatus?,
        pageable: Pageable,
    ): Slice<EnrollmentEntity>
}
