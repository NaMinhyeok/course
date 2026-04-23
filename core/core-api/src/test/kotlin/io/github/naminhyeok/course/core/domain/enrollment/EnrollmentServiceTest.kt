package io.github.naminhyeok.course.core.domain.enrollment

import io.github.naminhyeok.course.ContextTest
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.OffsetLimit
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.enums.EnrollmentStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentEntity
import io.github.naminhyeok.course.storage.db.core.enrollment.EnrollmentRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

private val cancelPolicyZone = ZoneId.of("Asia/Seoul")
private val cancelPolicyNow = LocalDateTime.of(2026, 4, 30, 10, 0)

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

    private fun assertCoreError(expected: ErrorType, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(expected)
    }

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

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        fun clock(): Clock {
            return Clock.fixed(cancelPolicyNow.atZone(cancelPolicyZone).toInstant(), cancelPolicyZone)
        }
    }

    @Test
    fun `수강 신청은 공개된 강의에 대기 상태 신청을 만들고 좌석을 차감한다`() {
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
    fun `수강 신청은 DRAFT 상태의 강의에 대해 COURSE_NOT_OPEN 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.DRAFT)

        assertCoreError(ErrorType.COURSE_NOT_OPEN) {
            enrollmentService.enroll(user, course.id)
        }
    }

    @Test
    fun `수강 신청은 CLOSED 상태의 강의에 대해 COURSE_NOT_OPEN 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.CLOSED)

        assertCoreError(ErrorType.COURSE_NOT_OPEN) {
            enrollmentService.enroll(user, course.id)
        }
    }

    @Test
    fun `수강 신청은 정원이 가득 찬 강의에 대해 CAPACITY_EXCEEDED 예외를 던진다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN, capacity = 1, reservedCount = 1)

        assertCoreError(ErrorType.CAPACITY_EXCEEDED) {
            enrollmentService.enroll(user, course.id)
        }
    }

    @Test
    fun `수강 신청은 존재하지 않는 강의에 대해 NOT_FOUND_DATA 예외를 던진다`() {
        assertCoreError(ErrorType.NOT_FOUND_DATA) {
            enrollmentService.enroll(user, 99999L)
        }
    }

    @Test
    fun `수강 확정은 대기 중인 신청을 확정 상태로 전이한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)

        enrollmentService.confirm(user, enrollmentId)

        val enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.CONFIRMED)
    }

    @Test
    fun `수강 확정은 본인 신청만 확정할 수 있다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        val other = User(id = 999L)

        assertCoreError(ErrorType.ACCESS_DENIED) {
            enrollmentService.confirm(other, enrollmentId)
        }
    }

    @Test
    fun `수강 확정은 존재하지 않는 신청에 대해 NOT_FOUND_DATA 예외를 던진다`() {
        assertCoreError(ErrorType.NOT_FOUND_DATA) {
            enrollmentService.confirm(user, 99999L)
        }
    }

    @Test
    fun `수강 확정은 이미 확정된 신청에 대해 실패한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.confirm(user, enrollmentId)

        assertThatThrownBy { enrollmentService.confirm(user, enrollmentId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `수강 취소는 확정된 신청을 취소하고 좌석을 복구한다`() {
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
    fun `취소된 신청도 최초 확정 시각을 보존한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.confirm(user, enrollmentId)
        val confirmedAt = enrollmentRepository.findById(enrollmentId).orElseThrow().confirmedAt

        enrollmentService.cancel(user, enrollmentId)

        val found = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(confirmedAt).isNotNull()
        assertThat(found.enrollmentStatus).isEqualTo(EnrollmentStatus.CANCELLED)
        assertThat(found.confirmedAt).isEqualTo(confirmedAt)
    }

    @Test
    fun `수강 취소는 확정 후 7일까지 가능하다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN, reservedCount = 1)
        val enrollment =
            enrollmentRepository.save(
                EnrollmentEntity(
                    courseId = course.id,
                    userId = user.id,
                    enrollmentStatus = EnrollmentStatus.CONFIRMED,
                    confirmedAt = cancelPolicyNow.minusDays(7),
                ),
            )

        enrollmentService.cancel(user, enrollment.id)

        val found = enrollmentRepository.findById(enrollment.id).orElseThrow()
        assertThat(found.enrollmentStatus).isEqualTo(EnrollmentStatus.CANCELLED)
        assertThat(courseSeatsRepository.findByCourseId(course.id)?.reservedCount).isEqualTo(0)
    }

    @Test
    fun `수강 취소는 확정 후 8일째부터 실패하고 좌석을 유지한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN, reservedCount = 1)
        val enrollment =
            enrollmentRepository.save(
                EnrollmentEntity(
                    courseId = course.id,
                    userId = user.id,
                    enrollmentStatus = EnrollmentStatus.CONFIRMED,
                    confirmedAt = cancelPolicyNow.minusDays(8),
                ),
            )

        assertCoreError(ErrorType.ENROLLMENT_CANCEL_EXPIRED) {
            enrollmentService.cancel(user, enrollment.id)
        }

        val found = enrollmentRepository.findById(enrollment.id).orElseThrow()
        assertThat(found.enrollmentStatus).isEqualTo(EnrollmentStatus.CONFIRMED)
        assertThat(courseSeatsRepository.findByCourseId(course.id)?.reservedCount).isEqualTo(1)
    }

    @Test
    fun `수강 취소는 대기 상태 신청에 대해 실패하고 좌석을 유지한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)

        assertThatThrownBy { enrollmentService.cancel(user, enrollmentId) }
            .isInstanceOf(IllegalStateException::class.java)

        val enrollment = enrollmentRepository.findById(enrollmentId).orElseThrow()
        assertThat(enrollment.enrollmentStatus).isEqualTo(EnrollmentStatus.PENDING)
        val seats = courseSeatsRepository.findByCourseId(course.id)
        assertThat(seats?.reservedCount).isEqualTo(1)
    }

    @Test
    fun `수강 취소는 본인 신청만 취소할 수 있다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        val other = User(id = 999L)

        assertCoreError(ErrorType.ACCESS_DENIED) {
            enrollmentService.cancel(other, enrollmentId)
        }
    }

    @Test
    fun `수강 취소는 이미 취소된 신청에 대해 실패한다`() {
        val course = saveCourseWithSeats(status = CourseStatus.OPEN)
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.confirm(user, enrollmentId)
        enrollmentService.cancel(user, enrollmentId)

        assertThatThrownBy { enrollmentService.cancel(user, enrollmentId) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `내 신청 목록 조회는 현재 사용자의 신청을 최신순으로 반환한다`() {
        val course1 = saveCourseWithSeats()
        val course2 = saveCourseWithSeats()
        val firstId = enrollmentService.enroll(user, course1.id)
        val secondId = enrollmentService.enroll(user, course2.id)

        val result = enrollmentService.getEnrollments(user, null, OffsetLimit(0, 10))

        assertThat(result.content).extracting("id").containsExactly(secondId, firstId)
        assertThat(result.content[0].course.id).isEqualTo(course2.id)
        assertThat(result.content[0].course.title).isEqualTo("강의")
        assertThat(result.content[0].status).isEqualTo(EnrollmentStatus.PENDING)
        assertThat(result.content[0].userId).isEqualTo(user.id)
        assertThat(result.content[0].appliedAt).isNotNull()
        assertThat(result.hasNext).isFalse()
    }

    @Test
    fun `내 신청 목록 조회는 상태 조건이 있으면 해당 상태만 반환한다`() {
        val course1 = saveCourseWithSeats()
        val course2 = saveCourseWithSeats()
        val pendingId = enrollmentService.enroll(user, course1.id)
        val confirmId = enrollmentService.enroll(user, course2.id)
        enrollmentService.confirm(user, confirmId)

        val result = enrollmentService.getEnrollments(user, EnrollmentStatus.CONFIRMED, OffsetLimit(0, 10))

        assertThat(result.content).extracting("id").containsExactly(confirmId)
        assertThat(pendingId).isNotEqualTo(confirmId)
        assertThat(result.hasNext).isFalse()
    }

    @Test
    fun `내 신청 목록 조회는 다른 사용자의 신청을 제외한다`() {
        val course = saveCourseWithSeats()
        val myId = enrollmentService.enroll(user, course.id)
        val other = User(id = 999L)
        val otherCourse = saveCourseWithSeats()
        enrollmentService.enroll(other, otherCourse.id)

        val result = enrollmentService.getEnrollments(user, null, OffsetLimit(0, 10))

        assertThat(result.content).extracting("id").containsExactly(myId)
    }

    @Test
    fun `내 신청 목록 조회는 신청이 없으면 빈 페이지를 반환한다`() {
        val result = enrollmentService.getEnrollments(user, null, OffsetLimit(0, 10))

        assertThat(result.content).isEmpty()
        assertThat(result.hasNext).isFalse()
    }

    @Test
    fun `내 신청 목록 조회는 페이지 경계와 hasNext 를 함께 계산한다`() {
        val course1 = saveCourseWithSeats()
        val course2 = saveCourseWithSeats()
        val course3 = saveCourseWithSeats()
        val firstId = enrollmentService.enroll(user, course1.id)
        val secondId = enrollmentService.enroll(user, course2.id)
        val thirdId = enrollmentService.enroll(user, course3.id)

        val firstPage = enrollmentService.getEnrollments(user, null, OffsetLimit(0, 2))
        val secondPage = enrollmentService.getEnrollments(user, null, OffsetLimit(1, 1))

        assertThat(firstPage.content).extracting("id").containsExactly(thirdId, secondId)
        assertThat(firstPage.hasNext).isTrue()
        assertThat(secondPage.content).extracting("id").containsExactly(secondId)
        assertThat(secondPage.hasNext).isTrue()
        assertThat(secondPage.content).extracting("id").doesNotContain(firstId)
    }

    @Test
    fun `확정 수강생 조회는 강의 생성자 요청에 대해 확정 신청을 최신순으로 반환한다`() {
        val course = saveCourseWithSeats()
        val pendingUser = User(id = 201L)
        val firstConfirmedUser = User(id = 202L)
        val secondConfirmedUser = User(id = 203L)

        enrollmentService.enroll(pendingUser, course.id)

        val firstConfirmedId = enrollmentService.enroll(firstConfirmedUser, course.id)
        enrollmentService.confirm(firstConfirmedUser, firstConfirmedId)

        val secondConfirmedId = enrollmentService.enroll(secondConfirmedUser, course.id)
        enrollmentService.confirm(secondConfirmedUser, secondConfirmedId)

        val result = enrollmentService.getConfirmedCourseEnrollments(creator, course.id)

        assertThat(result).hasSize(2)
        assertThat(result[0].id).isEqualTo(secondConfirmedId)
        assertThat(result[1].id).isEqualTo(firstConfirmedId)
        assertThat(result[0].status).isEqualTo(EnrollmentStatus.CONFIRMED)
        assertThat(result[1].status).isEqualTo(EnrollmentStatus.CONFIRMED)
    }

    @Test
    fun `확정 수강생 조회는 강의 생성자가 아니면 ACCESS_DENIED로 실패한다`() {
        val course = saveCourseWithSeats()
        val other = User(id = 999L)

        assertThatThrownBy { enrollmentService.getConfirmedCourseEnrollments(other, course.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ACCESS_DENIED)
    }

    @Test
    fun `확정 수강생 조회는 존재하지 않는 강의에 대해 NOT_FOUND_DATA로 실패한다`() {
        assertThatThrownBy { enrollmentService.getConfirmedCourseEnrollments(creator, 99999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }

    @Test
    fun `확정 수강생 조회는 확정 신청이 없으면 빈 목록을 반환한다`() {
        val course = saveCourseWithSeats()

        val result = enrollmentService.getConfirmedCourseEnrollments(creator, course.id)

        assertThat(result).isEmpty()
    }

    @Test
    fun `확정 수강생 조회는 확정 신청이 있는 강의에서 좌석 정보가 없으면 NOT_FOUND_DATA로 실패한다`() {
        val course = saveCourseWithSeats()
        val enrollmentId = enrollmentService.enroll(user, course.id)
        enrollmentService.confirm(user, enrollmentId)
        val seats = courseSeatsRepository.findByCourseId(course.id)!!
        courseSeatsRepository.delete(seats)

        assertThatThrownBy { enrollmentService.getConfirmedCourseEnrollments(creator, course.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }
}
