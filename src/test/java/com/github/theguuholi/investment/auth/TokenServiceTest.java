package com.github.theguuholi.investment.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
            "test-secret-key-that-is-long-enough-for-hs256-algorithm",
            3600L
        );
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        String token = tokenService.generateToken("alice@example.com");
        assertThat(token).isNotBlank();
    }

    @Test
    void extractEmail_fromValidToken_returnsEmail() {
        String token = tokenService.generateToken("alice@example.com");
        String email = tokenService.extractEmail(token);
        assertThat(email).isEqualTo("alice@example.com");
    }

    @Test
    void isValid_withValidToken_returnsTrue() {
        String token = tokenService.generateToken("alice@example.com");
        assertThat(tokenService.isValid(token)).isTrue();
    }

    @Test
    void isValid_withTamperedToken_returnsFalse() {
        String token = tokenService.generateToken("alice@example.com") + "tampered";
        assertThat(tokenService.isValid(token)).isFalse();
    }

    @Test
    void generateToken_withNullEmail_throwsException() {
        assertThatThrownBy(() -> tokenService.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
