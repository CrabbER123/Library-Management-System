package com.library.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.library.common.exception.ErrorCode;
import com.library.common.result.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 在 Servlet 过滤器/安全处理器中直接写出统一错误响应。
 */
@Component
public class RestErrorWriter {

    private final ObjectMapper objectMapper;

    public RestErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(code.code(), code.message()));
    }
}
