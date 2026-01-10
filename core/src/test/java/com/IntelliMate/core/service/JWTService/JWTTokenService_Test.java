package com.IntelliMate.core.service.JWTService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import javax.crypto.SecretKey;
import java.util.Date;



class JWTTokenService_Test 
{
    private JWTTokenService jwtTokenService;
    private SecretKey validTestKey;
    private SecretKey attackerKey;

    
    
    @BeforeEach
    void setUp() 
    {
        validTestKey = Jwts.SIG.HS256.key().build();
        attackerKey = Jwts.SIG.HS256.key().build();

        jwtTokenService = org.mockito.Mockito.mock(JWTTokenService.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(jwtTokenService, "SECRET_KEY", validTestKey);
    }

    @Test
    @DisplayName("GIVEN a user identifier WHEN generating token THEN return a valid 3-part JWT")
    void generateToken_ShouldReturnValidString() 
    {
        String userId = "user_abc_123";
        String token = jwtTokenService.generateToken(userId);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("GIVEN a valid token WHEN extracting user info THEN return correct subject")
    void extractUserInfo_ShouldReturnSubject() 
    {
        String originalUser = "admin@intellimate.com";
        String token = jwtTokenService.generateToken(originalUser);

        String extractedUser = jwtTokenService.extractUserInfo(token);

        assertThat(extractedUser).isEqualTo(originalUser);
    }

    @Test
    @DisplayName("GIVEN a token signed by a different key WHEN validating THEN return false")
    void isValid_ShouldReturnFalseForWrongSignature() 
    {
        // Token signed with a different key (Attacker scenario)
        String fraudulentToken = Jwts.builder()
                .subject("victim-user")
                .signWith(attackerKey)
                .compact();

        boolean result = jwtTokenService.isValid(fraudulentToken);

        assertThat(result).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid.token.structure", "eyJhbGciOiJIUzI1NiJ9.payload.signature"})
    @DisplayName("GIVEN malformed strings WHEN validating THEN return false")
    void isValid_ShouldReturnFalseForMalformedTokens(String malformedToken) 
    {
        assertThat(jwtTokenService.isValid(malformedToken)).isFalse();
    }

    @Test
    @DisplayName("GIVEN an expired token WHEN checking expiry THEN return true")
    void isExpired_ShouldReturnTrueForOldTokens() 
    {
        // Create a token that expired 1 hour ago
        String expiredToken = Jwts.builder()
                .subject("old-user")
                .expiration(new Date(System.currentTimeMillis() - 3600000)) 
                .signWith(validTestKey)
                .compact();

        assertThat(jwtTokenService.isExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("GIVEN a null token WHEN checking validity THEN should not throw exception")
    void isValid_ShouldHandleNullGracefully() 
    {
        assertDoesNotThrow(() -> 
        {
            boolean result = jwtTokenService.isValid(null);
            assertThat(result).isFalse();
        });
    }
}






