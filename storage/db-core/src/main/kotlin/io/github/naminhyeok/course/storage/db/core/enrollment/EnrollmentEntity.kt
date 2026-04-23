package io.github.naminhyeok.course.storage.db.core.enrollment

import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "enrollment",
    indexes = [
        Index(name = "idx_enrollment_user_id", columnList = "userId"),
        Index(name = "idx_enrollment_course_id_enrollment_status", columnList = "courseId, enrollmentStatus"),
    ],
)
class EnrollmentEntity(
    val courseId: Long,
    val userId: Long,
    enrollmentStatus: EnrollmentStatus = EnrollmentStatus.PENDING,
    confirmedAt: LocalDateTime? = null,
) : BaseEntity() {
    @Enumerated(EnumType.STRING)
    final var enrollmentStatus: EnrollmentStatus = enrollmentStatus
        private set

    var confirmedAt: LocalDateTime? = confirmedAt
        protected set

    fun confirm() {
        check(enrollmentStatus == EnrollmentStatus.PENDING) {
            "PENDING 상태에서만 확정할 수 있습니다: $enrollmentStatus"
        }
        enrollmentStatus = EnrollmentStatus.CONFIRMED
        confirmedAt = LocalDateTime.now()
    }

    fun cancel() {
        check(enrollmentStatus == EnrollmentStatus.CONFIRMED) {
            "CONFIRMED 상태에서만 취소할 수 있습니다: $enrollmentStatus"
        }
        enrollmentStatus = EnrollmentStatus.CANCELLED
    }
}
