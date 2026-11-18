package com.library.security;

import com.library.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 Authorization: Bearer &lt;token&gt;，成功则写入 SecurityContext。<br>
 * 令牌无效/过期时直接返回 401，不继续执行后续过滤器。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final RestErrorWriter errorWriter;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, RestErrorWriter errorWriter) {
        this.tokenProvider = tokenProvider;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(PREFIX.length()).trim();
        try {
            Claims claims = tokenProvider.parse(token).getPayload();
            AuthUser user = new AuthUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    readRoles(claims));
            var authentication = new UsernamePasswordAuthenticationToken(user, null,
                    user.roleCodes().stream().map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException ex) {
            log.debug("access token 已过期: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            errorWriter.write(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("access token 无效: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            errorWriter.write(response, ErrorCode.INVALID_TOKEN);
            return;
        }
        chain.doFilter(request, response);
    }

    private List<String> readRoles(Claims claims) {
        Object value = claims.get("roles");
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }
}
