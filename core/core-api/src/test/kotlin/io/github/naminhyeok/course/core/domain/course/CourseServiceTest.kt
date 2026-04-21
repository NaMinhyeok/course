package io.github.naminhyeok.course.core.domain.course

import io.github.naminhyeok.course.ContextTest
import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.enums.CourseStatus
import io.github.naminhyeok.course.storage.db.core.course.CourseEntity
import io.github.naminhyeok.course.storage.db.core.course.CourseRepository
import io.github.naminhyeok.course.storage.db.core.course.CourseSeatsRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Transactional
class CourseServiceTest(
    private val courseService: CourseService,
    private val courseRepository: CourseRepository,
    private val courseSeatsRepository: CourseSeatsRepository,
) : ContextTest() {
    private val creator = User(id = 100L)
    private val baseStart = LocalDateTime.of(2026, 5, 1, 0, 0)

    private fun content(capacity: Int = 10) =
        CourseContent(
            title = "강의",
            description = "설명",
            price = BigDecimal("10000"),
            capacity = capacity,
            startAt = baseStart,
            endAt = baseStart.plusDays(7),
        )

    private fun courseEntityOf(
        creatorId: Long = creator.id,
        status: CourseStatus = CourseStatus.DRAFT,
    ) = CourseEntity(
        creatorId = creatorId,
        title = "강의",
        description = "설명",
        price = BigDecimal("10000"),
        capacity = 10,
        startAt = baseStart,
        endAt = baseStart.plusDays(7),
        courseStatus = status,
    )

    @Test
    fun `createCourse 는 DRAFT 상태로 저장하고 id 를 반환한다`() {
        val courseId = courseService.createCourse(creator, content())

        val saved = courseRepository.findById(courseId).orElseThrow()
        assertThat(saved.creatorId).isEqualTo(creator.id)
        assertThat(saved.courseStatus).isEqualTo(CourseStatus.DRAFT)
        assertThat(saved.title).isEqualTo("강의")
    }

    @Test
    fun `createCourse 는 CourseSeats 를 capacity 와 함께 생성하고 reservedCount 는 0 이다`() {
        val courseId = courseService.createCourse(creator, content(capacity = 30))

        val seats = courseSeatsRepository.findByCourseId(courseId)
        assertThat(seats).isNotNull
        assertThat(seats?.capacity).isEqualTo(30)
        assertThat(seats?.reservedCount).isEqualTo(0)
    }

    @Test
    fun `openCourse 는 DRAFT 를 OPEN 으로 전이한다`() {
        val entity = courseRepository.save(courseEntityOf(status = CourseStatus.DRAFT))

        courseService.openCourse(creator, entity.id)

        assertThat(courseRepository.findById(entity.id).orElseThrow().courseStatus)
            .isEqualTo(CourseStatus.OPEN)
    }

    @Test
    fun `openCourse 는 소유자가 아니면 ACCESS_DENIED 예외를 던진다`() {
        val entity = courseRepository.save(courseEntityOf(creatorId = creator.id, status = CourseStatus.DRAFT))
        val other = User(id = 999L)

        assertThatThrownBy { courseService.openCourse(other, entity.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.ACCESS_DENIED)
    }

    @Test
    fun `openCourse 는 존재하지 않는 강의면 NOT_FOUND_DATA 예외를 던진다`() {
        assertThatThrownBy { courseService.openCourse(creator, 99999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }

    @Test
    fun `closeCourse 는 OPEN 을 CLOSED 로 전이한다`() {
        val entity = courseRepository.save(courseEntityOf(status = CourseStatus.OPEN))

        courseService.closeCourse(creator, entity.id)

        assertThat(courseRepository.findById(entity.id).orElseThrow().courseStatus)
            .isEqualTo(CourseStatus.CLOSED)
    }

    @Test
    fun `closeCourse 는 DRAFT 상태에서 호출하면 IllegalStateException 을 던진다`() {
        val entity = courseRepository.save(courseEntityOf(status = CourseStatus.DRAFT))

        assertThatThrownBy { courseService.closeCourse(creator, entity.id) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `openCourse 는 CLOSED 상태에서 호출하면 IllegalStateException 을 던진다`() {
        val entity = courseRepository.save(courseEntityOf(status = CourseStatus.CLOSED))

        assertThatThrownBy { courseService.openCourse(creator, entity.id) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `findCourses 는 status 필터 없이 호출하면 DRAFT 를 제외한다`() {
        val draft = courseRepository.save(courseEntityOf(status = CourseStatus.DRAFT))
        val open = courseRepository.save(courseEntityOf(status = CourseStatus.OPEN))
        val closed = courseRepository.save(courseEntityOf(status = CourseStatus.CLOSED))

        val courses = courseService.findCourses(status = null)

        assertThat(courses.map { it.id }).containsExactlyInAnyOrder(open.id, closed.id)
        assertThat(courses.map { it.id }).doesNotContain(draft.id)
    }

    @Test
    fun `findCourses 에 OPEN 을 지정하면 OPEN 만 반환한다`() {
        val open = courseRepository.save(courseEntityOf(status = CourseStatus.OPEN))
        courseRepository.save(courseEntityOf(status = CourseStatus.CLOSED))
        courseRepository.save(courseEntityOf(status = CourseStatus.DRAFT))

        val courses = courseService.findCourses(status = CourseStatus.OPEN)

        assertThat(courses).hasSize(1)
        assertThat(courses.first().id).isEqualTo(open.id)
    }

    @Test
    fun `findCourse 는 공개된 강의를 반환한다`() {
        val entity = courseRepository.save(courseEntityOf(status = CourseStatus.OPEN))

        val course = courseService.findCourse(entity.id)

        assertThat(course.id).isEqualTo(entity.id)
        assertThat(course.status).isEqualTo(CourseStatus.OPEN)
    }

    @Test
    fun `findCourse 는 DRAFT 강의면 NOT_FOUND_DATA 예외를 던진다`() {
        val entity = courseRepository.save(courseEntityOf(status = CourseStatus.DRAFT))

        assertThatThrownBy { courseService.findCourse(entity.id) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }

    @Test
    fun `findCourse 는 존재하지 않는 id 면 NOT_FOUND_DATA 예외를 던진다`() {
        assertThatThrownBy { courseService.findCourse(99999L) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.NOT_FOUND_DATA)
    }
}
