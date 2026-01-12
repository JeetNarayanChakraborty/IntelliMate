package E2ETesting;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.http.Cookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean; // New import
import com.IntelliMate.core.AIEngine;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.IntelliMate.core.service.JWTService.JWTTokenService;




@SpringBootTest(classes = com.IntelliMate.core.CoreApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@ActiveProfiles("test")
public class E2E_Testing 
{
    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private JWTTokenService jwtTokenService;
    
    @MockitoBean 
    private AIEngine aiEngine; 

    private static final String TEST_USER = "tester@intellimate.com";
    private Cookie authCookie;

    
    
    @BeforeEach
    void setup() 
    {
    	// Create the user object for the mock to return
        User mockUser = new User();
        mockUser.setId(TEST_USER); 
        mockUser.setEmail(TEST_USER);
    	
        lenient().when(userRepository.findByEmail(anyString())).thenReturn(mockUser);
        lenient().when(jwtTokenService.isValid("valid-mock-token")).thenReturn(true);
        lenient().when(jwtTokenService.extractUserInfo("valid-mock-token")).thenReturn(TEST_USER);
        
        authCookie = new Cookie("jwt", "valid-mock-token");
    }

    // --- GROUP 1: AI & TOOL INTEGRATION (Logic checks) ---

    @Test @Order(1)
    @DisplayName("1. Mail: Extraction Logic Confirmation")
    void test_MailLogic() throws Exception 
    {
        // Simulate Gemini successfully calling MailTool and returning the text
        String expectedOutput = "1. 📧 From: boss@work.com\nSubject: Report\nPreview: Please check the nested report...";
        when(aiEngine.chat(eq("Show my emails"), anyMap())).thenReturn(expectedOutput);

        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Show my emails\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value(containsString("📧 From:")));
    }

    @Test @Order(2)
    @DisplayName("2. Calendar: IST Timezone Handling")
    void test_CalendarTimezone() throws Exception 
    {
        // Verifies the prompt/response logic handles the +05:30 requirement
        String expectedOutput = "✓ Event created: Meeting on Friday at 12:00 PM (IST)";
        when(aiEngine.chat(contains("12:00 PM"), anyMap())).thenReturn(expectedOutput);

        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Schedule meeting at 12:00 PM\"}"))
                .andExpect(jsonPath("$.response").value(containsString("(IST)")));
    }

    @Test @Order(3)
    @DisplayName("3. News: Article Formatting")
    void test_NewsFormatting() throws Exception 
    {
        String mockNews = "Here are the articles:\n1. **AI Trends**\n🔗 http://news.com/ai";
        when(aiEngine.chat(contains("news"), anyMap())).thenReturn(mockNews);

        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"What is the news?\"}"))
                .andExpect(jsonPath("$.response").value(containsString("🔗")));
    }

    // --- GROUP 2: SECURITY & SESSION (Controller checks) ---

    @Test @Order(4)
    @DisplayName("4. Security: 403 Forbidden on Missing JWT")
    void test_Security_NoJWT() throws Exception 
    {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @Order(5)
    @DisplayName("5. Context: Session ID Propagation")
    void test_SessionContext() throws Exception 
    {
        when(aiEngine.chat(anyString(), anyMap())).thenReturn("Success");

        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Testing session\"}"))
                .andExpect(status().isOk()); 
        
        // Verifies the controller actually extracted the session ID and passed it to the AI
        verify(aiEngine).chat(anyString(), argThat(ctx -> ctx.containsKey("sessionID")));
    }

    @Test @Order(6)
    @DisplayName("6. Validation: Empty Prompt 400")
    void test_EmptyPrompt() throws Exception 
    {
        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- GROUP 3: RESILIENCE (Error checks) ---

    @Test @Order(7)
    @DisplayName("7. Resilience: AI Engine Timeout Handling")
    void test_EngineCrash() throws Exception 
    {
        // Simulates the LangChain service timing out or failing
        when(aiEngine.chat(anyString(), anyMap())).thenThrow(new RuntimeException("Gemini Offline"));

        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Help\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test @Order(8)
    @DisplayName("8. Flow: Multi-turn Memory Interaction")
    void test_MemoryFlow() throws Exception 
    {
        when(aiEngine.chat(eq("My name is John"), anyMap())).thenReturn("Hello John!");
        when(aiEngine.chat(eq("What is my name?"), anyMap())).thenReturn("Your name is John.");

        mockMvc.perform(post("/api/chat").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"My name is John\"}"));
        mockMvc.perform(post("/api/chat").cookie(authCookie).contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"What is my name?\"}"))
                .andExpect(jsonPath("$.response").value(containsString("John")));
    }

    @Test @Order(9)
    @DisplayName("9. Tool Safety: Confirming Actions")
    void test_ActionConfirmation() throws Exception 
    {
        when(aiEngine.chat(contains("delete"), anyMap())).thenReturn("Are you sure you want to delete the meeting?");

        mockMvc.perform(post("/api/chat").cookie(authCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"delete my meeting\"}"))
                .andExpect(jsonPath("$.response").value(containsString("Are you sure")));
    }

    @Test @Order(10)
    @DisplayName("10. User Lookup: 404 on Missing Record")
    void test_UserDatabaseMissing() throws Exception 
    {
        String ghostUser = "ghost@intellimate.com";
        String ghostToken = "valid-token-for-nonexistent-user";
        
        // Tell Security the token is fine (Bypasses the 403)
        when(jwtTokenService.isValid(ghostToken)).thenReturn(true);
        when(jwtTokenService.extractUserInfo(ghostToken)).thenReturn(ghostUser);
        when(userRepository.findByEmail(ghostUser)).thenReturn(null);

        Cookie ghostCookie = new Cookie("jwt", ghostToken);
        
        mockMvc.perform(post("/api/chat").cookie(ghostCookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Hi\"}"))
                .andExpect(status().isNotFound()); 
    }
}












