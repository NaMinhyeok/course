package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "enrollment",
    indexes = [
        Index(name = "idx_enrollment_user_id", columnList = "userId"),
    ],
)
class EnrollmentEntity(
    val courseId: Long,
    val userId: Long,
    enrollmentStatus: EnrollmentStatus = EnrollmentStatus.PENDING,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    final var enrollmentStatus: EnrollmentStatus = enrollmentStatus
        private set

    fun confirm() {
        check(enrollmentStatus == EnrollmentStatus.PENDING) {
            "PENDING 상태에서만 확정할 수 있습니다: $enrollmentStatus"
        }
        enrollmentStatus = EnrollmentStatus.CONFIRMED
    }

    fun cancel() {
        check(enrollmentStatus == EnrollmentStatus.CONFIRMED) {
            "CONFIRMED 상태에서만 취소할 수 있습니다: $enrollmentStatus"
        }
        enrollmentStatus = EnrollmentStatus.CANCELLED
    }
}
