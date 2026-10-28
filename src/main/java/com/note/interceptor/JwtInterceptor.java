package com.note.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.note.common.AuthUser;
import com.note.common.Result;
import com.note.common.UserContext;
import com.note.user.User;
import com.note.user.UserMapper;
import com.note.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (token == null) {
            if (isPublicAnonymousRequest(request)) {
                return true;
            }
            writeUnauthorized(response);
            return false;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            Long id = claims.get("userId", Number.class).longValue();
            String role = claims.get("role", String.class);
            User user = userMapper.selectById(id);
            if (user == null || user.getStatus() == null || user.getStatus() != 1) {
                writeUnauthorized(response);
                return false;
            }
            int tokenVersion = claims.get("ver", Integer.class);
            if (user.getTokenVersion() != null && user.getTokenVersion() != tokenVersion) {
                writeUnauthorized(response);
                return false;
            }
            UserContext.set(new AuthUser(id, claims.getSubject(), role));
            return true;
        } catch (Exception e) {
            if (isPublicAnonymousRequest(request)) {
                return true;
            }
            writeUnauthorized(response);
            return false;
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String value = header.substring(7).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        String param = request.getParameter("access_token");
        if (param != null && !param.isBlank()) {
            return param.trim();
        }
        return null;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(401, "Unauthorized")));
    }

    private boolean isPublicAnonymousRequest(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/images/")) {
            return true;
        }
        return "/api/notes".equals(uri)
                || "/api/notes/rank".equals(uri)
                || uri.matches("/api/notes/\\d+")
                || uri.matches("/api/users/\\d+")
                || uri.matches("/api/users/\\d+/notes")
                || uri.matches("/api/users/\\d+/stats")
                || uri.matches("/api/follows/\\d+/followers")
                || uri.matches("/api/follows/\\d+/following")
                || uri.matches("/api/comments/\\d+");
    }
}
