package io.github.naminhyeok.course.core.support.error

import org.springframework.boot.logging.LogLevel
import org.springframework.http.HttpStatus

enum class ErrorType(
    val status: HttpStatus,
    val code: ErrorCode,
    val message: String,
    val logLevel: LogLevel,
) {
    DEFAULT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.E500, "알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", LogLevel.ERROR),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, ErrorCode.E400, "요청이 올바르지 않습니다.", LogLevel.INFO),
    NOT_FOUND_DATA(HttpStatus.BAD_REQUEST, ErrorCode.E401, "해당 데이터를 찾을 수 없습니다.", LogLevel.ERROR),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, ErrorCode.E403, "해당 작업을 수행할 권한이 없습니다.", LogLevel.INFO),
    CAPACITY_EXCEEDED(HttpStatus.CONFLICT, ErrorCode.E409, "강의 정원이 마감되었습니다.", LogLevel.INFO),
    ENROLLMENT_CONFLICT(HttpStatus.CONFLICT, ErrorCode.E409, "일시적 혼잡으로 신청에 실패했습니다. 잠시 후 다시 시도해주세요.", LogLevel.WARN),
    ENROLLMENT_CANCEL_EXPIRED(HttpStatus.BAD_REQUEST, ErrorCode.E400, "수강 취소 가능 기간이 지났습니다.", LogLevel.INFO),
    COURSE_NOT_OPEN(HttpStatus.CONFLICT, ErrorCode.E409, "신청 가능한 상태의 강의가 아닙니다.", LogLevel.INFO),
}
