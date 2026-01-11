package com.IntelliMate.core.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;



@ExtendWith(MockitoExtension.class)
class UserService_Test 
{
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final String TEST_EMAIL = "user@intellimate.com";

    
    
    @Test
    @DisplayName("GIVEN a valid email WHEN user exists THEN return correctly mapped UserDetails")
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() 
    {
        // Arrange
        User mockUser = new User();
        mockUser.setEmail(TEST_EMAIL);
        mockUser.setPassword("encoded_password");

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(mockUser);

        // Act
        UserDetails result = userService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(TEST_EMAIL);
        assertThat(result.getPassword()).isEqualTo("encoded_password");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        verify(userRepository, times(1)).findByEmail(TEST_EMAIL);
    }

    @Test
    @DisplayName("GIVEN an OAuth2 user (null password) WHEN loading THEN return UserDetails with default password string")
    void loadUserByUsername_ShouldHandleNullPassword_ForOAuth2Users() 
    {
        // Arrange
        User oauthUser = new User();
        oauthUser.setEmail(TEST_EMAIL);
        oauthUser.setPassword(null); // Simulating OAuth2 user

        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(oauthUser);

        // Act
        UserDetails result = userService.loadUserByUsername(TEST_EMAIL);

        // Assert
        assertThat(result.getPassword()).isEqualTo("OAUTH2_USER");
        assertThat(result.getUsername()).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("GIVEN a non-existent email WHEN loading THEN throw UsernameNotFoundException")
    void loadUserByUsername_ShouldThrowException_WhenUserNotFound() 
    {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(null);

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> 
        {
            userService.loadUserByUsername("unknown@test.com");
        });

        assertThat(exception.getMessage()).contains("User not found: unknown@test.com");
    }
}




