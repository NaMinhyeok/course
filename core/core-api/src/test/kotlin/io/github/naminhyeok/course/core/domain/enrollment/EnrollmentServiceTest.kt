package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.ContextTest
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Transactional
class EnrollmentServiceTest(
    private val enrollmentService: EnrollmentService,
    private val courseRepository: CourseRepository,
    private val courseSeatsRepository: CourseSeatsRepository,
    private val enrollmentRepository: EnrollmentRepository,
) : ContextTest() {
    private val user = User(id = 200L)
    private val creator = User(id = 100L)
    private val baseStart = LocalDateTime.of(2026, 5, 1, 0, 0)

    private fun saveCourseWithSeats(
        status: CourseStatus = CourseStatus.OPEN,
        capacity: Int = 10,
        reservedCount: Int = 0,
    ): CourseEntity {
        val course =
            courseRepository.save(
                CourseEntity(
                    creatorId = creator.id,
                    title = "강의",
                    description = "설명",
                    price = BigDecimal("10000"),
                    capacity = capacity,
                    startAt = baseStart,
                    endAt = baseStart.plusDays(7),
                    courseStatus = status,
                ),
            )
        courseSeatsRepository.save(
            CourseSeatsEntity(courseId = course.id, capacity = capacity, reservedCount = reservedCount),
        )
        return course
    }

    @Test
    fun `enroll 은 OPEN 강의에 PENDING Enrollment 를 생성하고 reservedCount 를 1 증가시킨다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)

        val enrollmentId = enrollmentService.enroll(user, course.id)

        val enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.PENDING)
        assertThat(enrollment.courseId).isEqualTo(course.id)
        assertThat(enrollment.userId).isEqualTo(user.id)
        val seats = courseSeatsRepository.findByCourseId(course.id)
        assertThat(seats?.reservedCount).isEqualTo(1)
    }

    @Test
    fun `enroll 은 DRAFT 강의면 COURSE_NOT_OPEN 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.DRAFT)

        assertThatThrownBy { enrollmentService.enroll(user, course.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_OPEN)
    }

    @Test
    fun `enroll 은 CLOSED 강의면 COURSE_NOT_OPEN 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.CLOSED)

        assertThatThrownBy { enrollmentService.enroll(user, course.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.COURSE_NOT_OPEN)
    }

    @Test
    fun `enroll 은 정원이 가득 찬 강의에 대해 CAPACITY_EXCEEDED 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN, capacity = 1, reservedCount = 1)

        assertThatThrownBy { enrollmentService.enroll(user, course.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.CAPACITY_EXCEEDED)
    }

    @Test
    fun `enroll 은 존재하지 않는 강의면 NOT_FOUND_DATA 예외를 던진다`() {
        assertThatThrownBy { enrollmentService.enroll(user, 99999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }

    @Test
    fun `confirm 은 PENDING Enrollment 를 CONFIRMED 로 전이한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)

        enrollmentService.confirm(user, enrollmentId)

        val enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CONFIRMED)
    }

    @Test
    fun `confirm 은 다른 사용자의 Enrollment 면 ACCESS_DENIED 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        val other = User(id = 999L)

        assertThatThrownBy { enrollmentService.confirm(other, enrollmentId) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ACCESS_DENIED)
    }

    @Test
    fun `confirm 은 존재하지 않는 Enrollment 면 NOT_FOUND_DATA 예외를 던진다`() {
        assertThatThrownBy { enrollmentService.confirm(user, 99999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }

    @Test
    fun `confirm 은 이미 CONFIRMED 인 Enrollment 에 대해 IllegalStateException 을 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.confirm(user, enrollmentId)

        assertThatThrownBy { enrollmentService.confirm(user, enrollmentId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `cancel 은 CONFIRMED Enrollment 를 CANCELLED 로 전이하고 reservedCount 를 1 감소시킨다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.confirm(user, enrollmentId)

        enrollmentService.cancel(user, enrollmentId)

        val enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CANCELLED)
        val seats = courseSeatsRepository.findByCourseId(course.id)
        assertThat(seats?.reservedCount).isEqualTo(0)
    }

    @Test
    fun `cancel 은 PENDING Enrollment 도 CANCELLED 로 전이하고 reservedCount 를 1 감소시킨다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)

        enrollmentService.cancel(user, enrollmentId)

        val enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CANCELLED)
        val seats = courseSeatsRepository.findByCourseId(course.id)
        assertThat(seats?.reservedCount).isEqualTo(0)
    }

    @Test
    fun `cancel 은 다른 사용자의 Enrollment 면 ACCESS_DENIED 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        val other = User(id = 999L)

        assertThatThrownBy { enrollmentService.cancel(other, enrollmentId) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ACCESS_DENIED)
    }

    @Test
    fun `cancel 은 이미 CANCELLED 인 Enrollment 에 대해 IllegalStateException 을 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.cancel(user, enrollmentId)

        assertThatThrownBy { enrollmentService.cancel(user, enrollmentId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `getEnrollments 는 현재 사용자의 모든 신청을 최신순 Enrollment 로 반환한다`() {
        val course1 = saveCourseWithSeats()
        val course2 = saveCourseWithSeats()
        val firstId = enrollmentService.enroll(user, course1.id)
        val secondId = enrollmentService.enroll(user, course2.id)

        val result = enrollmentService.getEnrollments(user, null)

        assertThat(result).extracting("id").containsExactly(secondId, firstId)
        assertThat(result[0].course.id).isEqualTo(course2.id)
        assertThat(result[0].course.title).isEqualTo("강의")
        assertThat(result[0].status).isEqualTo(EnrollmentStatus.PENDING)
        assertThat(result[0].userId).isEqualTo(user.id)
        assertThat(result[0].appliedAt).isNotNull()
    }

    @Test
    fun `getEnrollments 는 status 필터 적용 시 해당 상태만 반환한다`() {
        val course1 = saveCourseWithSeats()
        val course2 = saveCourseWithSeats()
        val pendingId = enrollmentService.enroll(user, course1.id)
        val confirmId = enrollmentService.enroll(user, course2.id)
        enrollmentService.confirm(user, confirmId)

        val result = enrollmentService.getEnrollments(user, EnrollmentStatus.CONFIRMED)

        assertThat(result).extracting("id").containsExactly(confirmId)
        assertThat(pendingId).isNotEqualTo(confirmId)
    }

    @Test
    fun `getEnrollments 는 다른 사용자의 신청은 반환하지 않는다`() {
        val course = saveCourseWithSeats()
        val myId = enrollmentService.enroll(user, course.id)
        val other = User(id = 999L)
        val otherCourse = saveCourseWithSeats()
        enrollmentService.enroll(other, otherCourse.id)

        val result = enrollmentService.getEnrollments(user, null)

        assertThat(result).extracting("id").containsExactly(myId)
    }

    @Test
    fun `getEnrollments 는 신청이 없으면 빈 리스트를 반환한다`() {
        val result = enrollmentService.getEnrollments(user, null)

        assertThat(result).isEmpty()
    }
}
