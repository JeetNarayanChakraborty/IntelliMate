package com.IntelliMate.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = com.IntelliMate.core.CoreApplication.class)
class UserRepository_Test 
{
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private final String TEST_EMAIL = "developer@intellimate.com";
    private final String GOOGLE_ID = "google-oauth-id-123";
    private final String REFRESH_TOKEN = "refresh_token_sample";
    private final String ACCESS_TOKEN = "access_token_sample";

    @BeforeEach
    void setUp() 
    {
        // Creating a User using the Google registration constructor logic
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(TEST_EMAIL);
        user.setGoogleId(GOOGLE_ID);
        user.setGoogleAccessToken(ACCESS_TOKEN);
        user.setGoogleRefreshToken(REFRESH_TOKEN);
        user.setAuthMethod("google");
        
        // Setting expiry to 1 hour in the future
        user.setGoogleTokenExpiry(LocalDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(1));
        user.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
        user.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));

        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find user by email and verify details")
    void test_FindByEmail() 
    {
        User found = userRepository.findByEmail(TEST_EMAIL);
        assertThat(found).isNotNull();
        assertThat(found.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(found.getGoogleId()).isEqualTo(GOOGLE_ID);
    }

    @Test
    @DisplayName("Should verify existence by Google Refresh Token")
    void test_ExistsByGoogleRefreshToken() 
    {
        boolean exists = userRepository.existsByGoogleRefreshToken(REFRESH_TOKEN);
        assertThat(exists).isTrue();
        
        boolean fakeExists = userRepository.existsByGoogleRefreshToken("invalid_token");
        assertThat(fakeExists).isFalse();
    }

    @Test
    @DisplayName("Should retrieve users with expired tokens")
    void test_FindUsersWithExpiredTokens() 
    {
        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        // Add a user whose token has already expired
        User expiredUser = new User();
        expiredUser.setId(UUID.randomUUID().toString());
        expiredUser.setEmail("expired@test.com");
        expiredUser.setGoogleTokenExpiry(currentTime.minusMinutes(10)); // Expired 10 mins ago
        expiredUser.setCreatedAt(currentTime);
        
        entityManager.persist(expiredUser);
        entityManager.flush();

        List<User> expiredUsers = userRepository.findUsersWithExpiredTokens(currentTime);
        
        assertThat(expiredUsers).isNotEmpty();
        assertThat(expiredUsers).extracting(User::getEmail).contains("expired@test.com");
    }

    @Test
    @DisplayName("Should retrieve users with valid tokens")
    void test_FindUsersWithValidTokens() 
    {
        LocalDateTime currentTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        
        List<User> validUsers = userRepository.findUsersWithValidTokens(currentTime);
        
        // The user created in setUp has a valid token
        assertThat(validUsers).hasSize(1);
        assertThat(validUsers.get(0).getEmail()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("Should find user with specific ID and valid token")
    void test_FindUserWithUserIDAndValidGoogleToken() 
    {
        User user = userRepository.findUserWithUserIDAndValidGoogleToken(TEST_EMAIL);
        
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(user.getGoogleAccessToken()).isNotNull();
    }

    @Test
    @DisplayName("Should delete user by email and verify removal")
    void test_DeleteByEmail() 
    {
        userRepository.deleteByEmail(TEST_EMAIL);
        entityManager.flush();
        entityManager.clear();

        assertThat(userRepository.findByEmail(TEST_EMAIL)).isNull();
    }
}