package io.github.naminhyeok.course.storage.db.core.enrollment

import org.springframework.data.jpa.repository.JpaRepository

interface EnrollmentRepository : JpaRepository<EnrollmentEntity, Long>
