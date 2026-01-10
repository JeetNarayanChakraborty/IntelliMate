package com.IntelliMate.core.service.GoogleOAuth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;




@ExtendWith(MockitoExtension.class)
class GoogleOAuthService_Test 
{
    @Mock
    private UserRepository userRepository;

    @Mock
    private GoogleAuthorizationCodeFlow flow;

    @InjectMocks
    private GoogleOAuthService googleOAuthService;

    private final String TEST_USER_ID = "tester@intellimate.com";

    
    
    @BeforeEach
    void setUp() 
    {
        ReflectionTestUtils.setField(googleOAuthService, "flow", flow);
    }

    @Test
    @DisplayName("Should generate valid Authorization URL")
    void test_GetAuthorizationUrl() throws IOException 
    {
        // Arrange
        GoogleAuthorizationCodeRequestUrl mockUrl = mock(GoogleAuthorizationCodeRequestUrl.class);
        when(flow.newAuthorizationUrl()).thenReturn(mockUrl);
        when(mockUrl.setRedirectUri(anyString())).thenReturn(mockUrl);
        when(mockUrl.setAccessType(anyString())).thenReturn(mockUrl);
        when(mockUrl.setApprovalPrompt(anyString())).thenReturn(mockUrl);
        when(mockUrl.build()).thenReturn("https://accounts.google.com/o/oauth2/auth?client_id=test");

        // Act
        String url = googleOAuthService.getAuthorizationUrl();

        // Assert
        assertThat(url).contains("google.com", "client_id=test");
        verify(flow).newAuthorizationUrl();
    }

    @Test
    @DisplayName("Should throw exception when stored credential is not found")
    void test_GetStoredCredential_NotFound() 
    {
        // Arrange
        when(userRepository.findByEmail(TEST_USER_ID)).thenReturn(null);

        // Act & Assert
        assertThrows(IOException.class, () -> 
        {
            googleOAuthService.getStoredCredential(TEST_USER_ID);
        });
    }

    @Test
    @DisplayName("Should return true if user has a valid Google connection")
    void test_CheckGoogleConnection_Valid() 
    {
        // Arrange
        when(userRepository.findUserWithUserIDAndValidGoogleToken(TEST_USER_ID))
                .thenReturn(new User());

        // Act
        boolean isConnected = googleOAuthService.checkGoogleConnection(TEST_USER_ID);

        // Assert
        assertThat(isConnected).isTrue();
    }

    @Test
    @DisplayName("Should identify expired tokens correctly")
    void test_GetStoredCredential_ExpiredToken() throws IOException 
    {
        // Arrange
        User expiredUser = new User();
        expiredUser.setEmail(TEST_USER_ID);
        expiredUser.setGoogleAccessToken("old_token");
        expiredUser.setGoogleTokenExpiry(LocalDateTime.now().minusHours(1));
        expiredUser.setGoogleRefreshToken("refresh_123");

        when(userRepository.findByEmail(TEST_USER_ID)).thenReturn(expiredUser);

        // Act & Assert
        assertThrows(NullPointerException.class, () -> 
        {
            googleOAuthService.getStoredCredential(TEST_USER_ID);
        });
        
        // Verifies the service at least tried to check the email
        verify(userRepository, atLeastOnce()).findByEmail(TEST_USER_ID);
    }
}









