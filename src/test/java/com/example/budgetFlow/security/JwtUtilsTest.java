package com.example.budgetFlow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils("TajniKljucZaJWTKojiJeDuzine32Bajt+", 3600000);
    }

    @Test
    void generateAndValidateToken() {
        UserDetails user = User.withUsername("test@mail.com")
                .password("x")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtils.generateToken(user);

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("test@mail.com", jwtUtils.getUsernameFromToken(token));
    }

    @Test
    void invalidToken_returnsFalse() {
        assertFalse(jwtUtils.validateJwtToken("not.a.valid.token"));
    }
}
