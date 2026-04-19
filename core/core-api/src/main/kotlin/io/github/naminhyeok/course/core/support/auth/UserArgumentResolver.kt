package io.github.naminhyeok.course.core.support.auth

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

class UserArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean = parameter.parameterType == User::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): User {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw CoreException(ErrorType.INVALID_REQUEST)
        val userId =
            request.getHeader("X-User-Id")
                ?: throw CoreException(ErrorType.INVALID_REQUEST)
        val id =
            userId.toLongOrNull()
                ?: throw CoreException(ErrorType.INVALID_REQUEST)
        return User(id)
    }
}
