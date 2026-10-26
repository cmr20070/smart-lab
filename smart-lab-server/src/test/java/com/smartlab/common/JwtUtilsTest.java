package com.smartlab.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // 通过反射注入 @Value 字段（unit 测试不启动 Spring 容器）
        try {
            var secretField = JwtUtils.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtils, "smart-lab-secret-key-2024-campus-laboratory-equipment-management");
            var expField = JwtUtils.class.getDeclaredField("expiration");
            expField.setAccessible(true);
            expField.set(jwtUtils, 86400000L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateToken_returnsToken_parseable() {
        String token = jwtUtils.generateToken(1L, "tester", "STUDENT");
        assertNotNull(token);
        assertFalse(token.isBlank());
        var claims = jwtUtils.parseToken(token);
        assertEquals("tester", claims.get("username", String.class));
        assertEquals("STUDENT", claims.get("role", String.class));
        assertEquals(1L, claims.get("userId", Long.class));
    }

    @Test
    void isTokenExpired_freshToken_shouldNotBeExpired() {
        String token = jwtUtils.generateToken(2L, "hello", "TEACHER");
        assertFalse(jwtUtils.isTokenExpired(token));
    }

    @Test
    void isTokenExpired_invalidToken_returnsTrue() {
        assertTrue(jwtUtils.isTokenExpired("not.a.jwt"));
        assertTrue(jwtUtils.isTokenExpired(null));
    }
}