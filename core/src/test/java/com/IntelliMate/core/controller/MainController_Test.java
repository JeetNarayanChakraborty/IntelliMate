package com.IntelliMate.core.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import com.IntelliMate.core.AIEngine;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.IntelliMate.core.service.EncryptionService.JasyptEncryptionService;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.IntelliMate.core.service.JWTService.JWTTokenService;
import com.IntelliMate.core.service.SystemMailService.SystemMailService;
import com.IntelliMate.core.service.UserService.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.mock.web.MockHttpSession;




@ExtendWith(MockitoExtension.class)
class MainController_Test 
{
    private MockMvc mockMvc;

    @Mock private AIEngine aiEngine;
    @Mock private GoogleOAuthService googleOAuthService;
    @Mock private JWTTokenService jwtTokenService;
    @Mock private UserRepository userRepository;
    @Mock private JasyptEncryptionService jasyptEncryptionService;
    @Mock private RememberMeServices rememberMeService;
    @Mock private SystemMailService systemMailService;
    @Mock private UserService userService;

    @InjectMocks
    private MainController mainController;

    
    
    
    @BeforeEach
    void setUp() 
    {
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("classpath:/templates/");
        viewResolver.setSuffix(".html");

        mockMvc = MockMvcBuilders.standaloneSetup(mainController)
                               .setViewResolvers(viewResolver)
                               .build();
    }
    
    @Test
    @DisplayName("Verify static page routing")
    void should_ReturnCorrectViews_ForStaticEndpoints() throws Exception 
    {
        mockMvc.perform(get("/api/home")).andExpect(view().name("HomePage"));
        mockMvc.perform(get("/api/")).andExpect(view().name("login"));
        mockMvc.perform(get("/api/registration")).andExpect(view().name("registration"));
    }

    @Test
    @DisplayName("Login Success: Should redirect to dashboard with JWT")
    void should_RedirectToDashboard_OnValidLogin() throws Exception 
    {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("encrypted_pass");

        when(userRepository.findByEmail("test@test.com")).thenReturn(user);
        when(jasyptEncryptionService.decrypt("encrypted_pass")).thenReturn("raw_pass");
        when(jwtTokenService.generateToken("test@test.com")).thenReturn("mock-jwt");

        mockMvc.perform(post("/api/UserLogin")
                .param("email", "test@test.com")
                .param("password", "raw_pass"))
               .andExpect(status().isFound())
               .andExpect(header().string("Location", containsString("dashboard?token=mock-jwt")));
    }

    @Test
    @DisplayName("Login Failure: Should redirect to login with error param")
    void should_RedirectToLogin_OnInvalidCredentials() throws Exception 
    {
        when(userRepository.findByEmail("wrong@test.com")).thenReturn(null);

        mockMvc.perform(post("/api/UserLogin")
                .param("email", "wrong@test.com")
                .param("password", "any"))
               .andExpect(status().isFound())
               .andExpect(header().string("Location", containsString("error=user_not_found")));
    }

    @Test
    @DisplayName("OAuth Callback: Handle new user registration via Google")
    void should_RegisterNewUser_OnGoogleCallback() throws Exception 
    {
        User googleUser = new User();
        googleUser.setEmail("new@google.com");
        googleUser.setGoogleId("g-123");

        when(googleOAuthService.exchangeCodeForTokens("code-123")).thenReturn(googleUser);
        when(userRepository.findByEmail("new@google.com")).thenReturn(null);
        when(jwtTokenService.generateToken("new@google.com")).thenReturn("jwt-123");

        mockMvc.perform(get("/api/oauth2/callback").param("code", "code-123"))
               .andExpect(status().isFound())
               .andExpect(header().string("Location", containsString("token=jwt-123")));

        verify(userRepository).save(any(User.class));
        verify(systemMailService).sendEmailWithHTML(eq("new@google.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Chat: Return 403 when JWT cookie is missing")
    void should_Return403_WhenCookieMissing() throws Exception 
    {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"hi\"}"))
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Chat: Success path with valid context and user")
    void should_ReturnAiResponse_WhenAuthorized() throws Exception 
    {
        String token = "valid-jwt";
        when(jwtTokenService.isValid(token)).thenReturn(true);
        when(jwtTokenService.extractUserInfo(token)).thenReturn("user@test.com");
        when(userRepository.findByEmail("user@test.com")).thenReturn(new User());
        when(aiEngine.chat(eq("Hello"), anyMap())).thenReturn("Hi User");

        mockMvc.perform(post("/api/chat")
                .cookie(new jakarta.servlet.http.Cookie("jwt", token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":\"Hello\"}"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.response").value("Hi User"));
    }

    @Test
    @DisplayName("Password Reset: Should update password and send confirmation mail")
    void should_ResetPassword_WhenSessionIsValid() throws Exception 
    {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("Email", "reset@test.com");
        
        User user = new User();
        when(userRepository.findByEmail("reset@test.com")).thenReturn(user);
        when(jasyptEncryptionService.encrypt("new-pass")).thenReturn("new-enc-pass");

        mockMvc.perform(post("/api/handleResetPassword")
                .session(session)
                .param("confirmPassword", "new-pass"))
               .andExpect(status().isFound());

        assertThat(user.getPassword()).isEqualTo("new-enc-pass");
        verify(systemMailService).sendEmail(eq("reset@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("Logout: Should invalidate session and clear cookies")
    void should_LogoutUser_Completely() throws Exception 
    {
        mockMvc.perform(get("/api/logout"))
               .andExpect(status().isFound())
               .andExpect(cookie().maxAge("jwt", 0))
               .andExpect(unmockedSessionCheck());
    }

    private org.springframework.test.web.servlet.ResultMatcher unmockedSessionCheck() 
    {
        return result -> assertThat(result.getRequest().getSession(false)).isNull();
    }
}










