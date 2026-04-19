package io.github.naminhyeok.course.core.support.auth

import io.github.naminhyeok.course.core.domain.User
import io.github.naminhyeok.course.core.support.error.CoreException
import io.github.naminhyeok.course.core.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class UserArgumentResolverTest {
    private val resolver = UserArgumentResolver()

    @Test
    fun `X-User-Id 헤더가 있으면 User 로 변환한다`() {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("X-User-Id") } returns "42"
        val webRequest = mockk<NativeWebRequest>()
        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

        val user = resolver.resolveArgument(mockk(), null, webRequest, null)

        assertThat(user.id).isEqualTo(42L)
    }

    @Test
    fun `X-User-Id 헤더가 없으면 INVALID_REQUEST 예외를 던진다`() {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("X-User-Id") } returns null
        val webRequest = mockk<NativeWebRequest>()
        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

        assertThatThrownBy { resolver.resolveArgument(mockk(), null, webRequest, null) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST)
    }

    @Test
    fun `X-User-Id 헤더가 숫자가 아니면 INVALID_REQUEST 예외를 던진다`() {
        val request = mockk<HttpServletRequest>()
        every { request.getHeader("X-User-Id") } returns "not-a-number"
        val webRequest = mockk<NativeWebRequest>()
        every { webRequest.getNativeRequest(HttpServletRequest::class.java) } returns request

        assertThatThrownBy { resolver.resolveArgument(mockk(), null, webRequest, null) }
            .isInstanceOf(CoreException::class.java)
            .extracting("errorType")
            .isEqualTo(ErrorType.INVALID_REQUEST)
    }

    @Test
    fun `parameter 타입이 User 일 때만 지원한다`() {
        val userParam = mockk<MethodParameter>()
        every { userParam.parameterType } returns User::class.java
        val otherParam = mockk<MethodParameter>()
        every { otherParam.parameterType } returns String::class.java

        assertThat(resolver.supportsParameter(userParam)).isTrue
        assertThat(resolver.supportsParameter(otherParam)).isFalse
    }
}
