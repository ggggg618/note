package com.note.util;

import com.note.common.BusinessException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void generatesAndParsesToken() {
        JwtUtil jwtUtil = new JwtUtil("0123456789abcdef0123456789abcdef", 1);
        String token = jwtUtil.generate(1L, "alice", "USER");
        Claims claims = jwtUtil.parse(token);
        assertEquals("alice", claims.getSubject());
        assertEquals(1L, claims.get("userId", Number.class).longValue());
        assertEquals("USER", claims.get("role", String.class));
    }

    @Test
    void rejectsInvalidToken() {
        JwtUtil jwtUtil = new JwtUtil("0123456789abcdef0123456789abcdef", 1);
        assertThrows(BusinessException.class, () -> jwtUtil.parse("not-a-token"));
    }
}
