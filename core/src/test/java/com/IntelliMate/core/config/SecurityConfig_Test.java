package com.IntelliMate.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class SecurityConfig_Test 
{
    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpSecurity httpSecurity;

    @InjectMocks
    private SecurityConfig securityConfig;

    @Test
    @DisplayName("Verify secret key retrieval from file with fallback mechanism")
    void should_ReturnSecretKey_When_FileIsProcessed() 
    {
        // Arrange: Preparation of a valid key and an error-inducing path
        String validKey = "test-secret-key-456";
        
        // Act & Assert: Verification of successful read
        try(MockedStatic<Files> mockedFiles = mockStatic(Files.class)) 
        {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn(validKey);
            String result = securityConfig.rememberMeSecretKey("/valid/path");
            assertThat(result).isEqualTo(validKey);
        }

        // Act & Assert: Verification of fallback logic on exception
        String fallback = securityConfig.rememberMeSecretKey("invalid/path");
        assertThat(fallback).isEqualTo("emergency-fallback-key-12345");
    }

    @Test
    @DisplayName("Verify configuration of TokenBasedRememberMeServices")
    void should_ReturnConfiguredRememberMeServices() 
    {
        // Act: Creation of the remember-me service bean
        RememberMeServices services = securityConfig.rememberMeServices("secret-key", userDetailsService);

        // Assert: Verification of service type and internal settings
        assertThat(services).isInstanceOf(TokenBasedRememberMeServices.class);
        TokenBasedRememberMeServices tokenService = (TokenBasedRememberMeServices) services;
        assertThat(tokenService.getParameter()).isEqualTo("remember-me");
    }

    @Test
    @DisplayName("Verify SecurityFilterChain bean instantiation")
    void should_BuildSecurityFilterChain() throws Exception 
    {
        // Arrange: Mocking of the HttpSecurity fluent API
        RememberMeServices mockServices = mock(RememberMeServices.class);
        when(httpSecurity.csrf(any())).thenReturn(httpSecurity);
        when(httpSecurity.sessionManagement(any())).thenReturn(httpSecurity);
        when(httpSecurity.authorizeHttpRequests(any())).thenReturn(httpSecurity);
        when(httpSecurity.rememberMe(any())).thenReturn(httpSecurity);

        // Act: Execution of the filter chain builder
        SecurityFilterChain filterChain = securityConfig.securityFilterChain(httpSecurity, mockServices);

        // Assert: Confirmation of successful build invocation
        verify(httpSecurity).build();
    }
}






