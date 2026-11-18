package com.library.security;

import com.library.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 已登录但无权限时返回 403 JSON。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final RestErrorWriter errorWriter;

    public RestAccessDeniedHandler(RestErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        errorWriter.write(response, ErrorCode.FORBIDDEN);
    }
}
