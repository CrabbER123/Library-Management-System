package com.library.security;

import com.library.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 未认证访问受保护资源时返回 401 JSON。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final RestErrorWriter errorWriter;

    public RestAuthenticationEntryPoint(RestErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        errorWriter.write(response, ErrorCode.UNAUTHORIZED);
    }
}
