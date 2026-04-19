package io.github.naminhyeok.course.core.api.controller

import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.github.naminhyeok.course.core.support.response.ApiResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.logging.LogLevel
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

@RestControllerAdvice
class ApiControllerAdvice {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CoreException::class)
    fun handleCoreException(e: CoreException): ResponseEntity<ApiResponse<Any>> {
        when (e.errorType.logLevel) {
            LogLevel.ERROR -> log.error("CoreException : {}", e.message, e)
            LogLevel.WARN -> log.warn("CoreException : {}", e.message, e)
            else -> log.info("CoreException : {}", e.message, e)
        }
        return ResponseEntity(ApiResponse.error(e.errorType, e.data), e.errorType.status)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(e: IllegalStateException): ResponseEntity<ApiResponse<Any>> {
        log.info("IllegalStateException : {}", e.message)
        return ResponseEntity(ApiResponse.error(ErrorType.INVALID_REQUEST), ErrorType.INVALID_REQUEST.status)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(e: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Any>> {
        log.info("HttpMessageNotReadableException : {}", e.message)
        return ResponseEntity(ApiResponse.error(ErrorType.INVALID_REQUEST), ErrorType.INVALID_REQUEST.status)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(e: MethodArgumentTypeMismatchException): ResponseEntity<ApiResponse<Any>> {
        log.info("MethodArgumentTypeMismatchException : {}", e.message)
        return ResponseEntity(ApiResponse.error(ErrorType.INVALID_REQUEST), ErrorType.INVALID_REQUEST.status)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Any>> {
        log.error("Exception : {}", e.message, e)
        return ResponseEntity(ApiResponse.error(ErrorType.DEFAULT_ERROR), ErrorType.DEFAULT_ERROR.status)
    }
}
