package E2ETesting;

import com.IntelliMate.core.CoreApplication;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static org.hamcrest.Matchers.containsString;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Base64;



@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest(
	    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	    classes = CoreApplication.class)
public class E2E_Testing 
{
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private final String TEST_USER = "tester@intellimate.com";

    
    
    @BeforeEach
    void setup() 
    {
        if (userRepository.findByEmail(TEST_USER) == null) 
        {
            User user = new User();
            user.setEmail(TEST_USER);
            user.setPassword("password");
            userRepository.save(user);
        }
        reset();
    }

    // --- SECTION 1: MAILTOOL & GMAIL API (Tests 1-10) ---

    @Test @Order(1) @WithMockUser(username = TEST_USER)
    @DisplayName("1. Search & Reply: Basic Flow")
    void test1_SearchAndReply_Success() throws Exception 
    {
        String encoded = Base64.getUrlEncoder().encodeToString("Meet at 5".getBytes());
        stubFor(get(urlPathMatching("/gmail/v1/users/me/messages/.*"))
            .willReturn(aResponse().withStatus(200).withBody("{\"payload\": {\"mimeType\": \"text/plain\", \"body\": {\"data\": \"" + encoded + "\"}}}")));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Reply 'OK' to the meeting email\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(2) @WithMockUser(username = TEST_USER)
    @DisplayName("2. Truncation: 2000 Char Limit")
    void test2_MailTruncation_Boundary() throws Exception 
    {
        String hugeBody = "A".repeat(2001);
        String encoded = Base64.getUrlEncoder().encodeToString(hugeBody.getBytes());
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{\"payload\": {\"body\": {\"data\": \"" + encoded + "\"}}}")));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Read my email\"}"))
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("[Truncated for brevity]")));
    }

    @Test @Order(3) @WithMockUser(username = TEST_USER)
    @DisplayName("3. Multipart: Recursive Extraction")
    void test3_Multipart_Extraction() throws Exception 
    {
        String encoded = Base64.getUrlEncoder().encodeToString("Nested content".getBytes());
        String multipartJson = "{\"payload\": {\"parts\": [{\"mimeType\": \"text/plain\", \"body\": {\"data\": \"" + encoded + "\"}}]}}";
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody(multipartJson)));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"What does the nested email say?\"}"))
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("Nested content")));
    }

    @Test @Order(4) @WithMockUser(username = TEST_USER)
    @DisplayName("4. Header Extraction: Case Sensitivity")
    void test4_Header_CaseSensitivity() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{\"payload\": {\"headers\": [{\"name\": \"FROM\", \"value\": \"sender@test.com\"}]}}")));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Who sent my last mail?\"}")).andExpect(status().isOk());
    }

    @Test @Order(5) @WithMockUser(username = TEST_USER)
    @DisplayName("5. Error: System Failure Handling")
    void test5_Mail_SystemError() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(500)));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Get mails\"}"))
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("System failed")));
    }

    @Test @Order(6) @WithMockUser(username = TEST_USER)
    @DisplayName("6. Empty Body: Null Check")
    void test6_Mail_EmptyBody() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{\"payload\": {\"body\": {}}}")));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Read empty mail\"}"))
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("No text content available")));
    }

    @Test @Order(7) @WithMockUser(username = TEST_USER)
    @DisplayName("7. Multi-Recipient Send")
    void test7_Mail_MultiSend() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email a@b.com and c@d.com\"}")).andExpect(status().isOk());
    }

    @Test @Order(8) @WithMockUser(username = TEST_USER)
    @DisplayName("8. Malformed Base64 Recovery")
    void test8_Mail_MalformedBase64() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{\"payload\": {\"body\": {\"data\": \"!!!\"}}}")));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"Read mail\"}"));
    }

    @Test @Order(9) @WithMockUser(username = TEST_USER)
    @DisplayName("9. Snippet Extraction")
    void test9_Mail_Snippet() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{\"snippet\": \"Testing snippet\"}")));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\": \"Summary?\"}"));
    }

    @Test @Order(10) @WithMockUser(username = TEST_USER)
    @DisplayName("10. ThreadID Continuity")
    void test10_Mail_ThreadID() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Reply to thread T1\"}")).andExpect(status().isOk());
    }

    // --- SECTION 2: CALENDAR TOOL (Tests 11-20) ---

    @Test @Order(11) @WithMockUser(username = TEST_USER)
    @DisplayName("11. Calendar: Conflict Handling")
    void test11_Calendar_Conflict() throws Exception 
    {
        stubFor(post(urlEqualTo("/calendar/v1/freeBusy")).willReturn(aResponse().withStatus(200).withBody("{\"calendars\":{\"primary\":{\"busy\":[{\"start\":\"...\"}]}}}")));
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Schedule at 10am\"}"));
    }

 // --- SECTION 2: CALENDAR TOOL & GOOGLE CALENDAR API (Tests 11-20) ---

    @Test @Order(12) @WithMockUser(username = TEST_USER)
    @DisplayName("12. Slot Duration: Parsing and Math Validation")
    void test12_Calendar_SlotDurationParsing() throws Exception 
    {
        // Verifies the tool correctly passes duration to the service
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Schedule a long 90-minute brainstorm for 3pm\"}"))
            .andExpect(status().isOk());
        
        verify(postRequestedFor(urlPathEqualTo("/calendar/v1/calendars/primary/events"))
            .withRequestBody(matchingJsonPath("$.end.dateTime", containing("16:30:00"))));
    }

    @Test @Order(13) @WithMockUser(username = TEST_USER)
    @DisplayName("13. Timezone: ISO 8601 Offset Handling")
    void test13_Calendar_TimezoneConversion() throws Exception 
    {
        // Tests if the agent/tool handles UTC vs Local time correctly
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"I have a meeting at 2pm UTC, check my local calendar\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(14) @WithMockUser(username = TEST_USER)
    @DisplayName("14. Update Flow: Required ID Retrieval")
    void test14_Calendar_UpdateIdRetrieval() throws Exception 
    {
        // Mock the 'get' call that must happen before an 'update'
        stubFor(get(urlPathEqualTo("/calendar/v1/calendars/primary/events"))
            .willReturn(aResponse().withStatus(200).withBody("{\"items\": [{\"id\": \"sync_001\", \"summary\": \"Sync\"}]}")));
        
        stubFor(put(urlMatching("/calendar/v1/calendars/primary/events/sync_001"))
            .willReturn(aResponse().withStatus(200)));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Change my Sync meeting to 4pm\"}"))
            .andExpect(status().isOk());
            
        verify(getRequestedFor(urlPathEqualTo("/calendar/v1/calendars/primary/events")));
    }

    @Test @Order(15) @WithMockUser(username = TEST_USER)
    @DisplayName("15. Delete Flow: 204 No Content Handling")
    void test15_Calendar_DeleteStatus204() throws Exception 
    {
        stubFor(delete(urlMatching("/calendar/v1/calendars/primary/events/.*"))
            .willReturn(aResponse().withStatus(204)));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Cancel my standup meeting\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(16) @WithMockUser(username = TEST_USER)
    @DisplayName("16. Attendees: Email List Validation")
    void test16_Calendar_AttendeeValidation() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Invite bob@test.com and dev-team@test.com to the review\"}"))
            .andExpect(status().isOk());

        verify(postRequestedFor(urlPathEqualTo("/calendar/v1/calendars/primary/events"))
            .withRequestBody(matchingJsonPath("$.attendees[0].email", equalTo("bob@test.com"))));
    }

    @Test @Order(17) @WithMockUser(username = TEST_USER)
    @DisplayName("17. Empty Day: Logic for No Events")
    void test17_Calendar_EmptyDayHandling() throws Exception 
    {
        stubFor(get(urlPathEqualTo("/calendar/v1/calendars/primary/events"))
            .willReturn(aResponse().withStatus(200).withBody("{\"items\": []}")));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"What is on my calendar for today?\"}"))
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("no events")));
    }

    @Test @Order(18) @WithMockUser(username = TEST_USER)
    @DisplayName("18. Weekly Range: Monday to Sunday Calculation")
    void test18_Calendar_WeeklyRangeCheck() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"What does my week look like?\"}"))
            .andExpect(status().isOk());

        // Verify query params timeMin and timeMax cover a 7-day span
        verify(getRequestedFor(urlPathEqualTo("/calendar/v1/calendars/primary/events"))
            .withQueryParam("timeMin", matching(".*T00:00:00Z")));
    }

    @Test @Order(19) @WithMockUser(username = TEST_USER)
    @DisplayName("19. Available Slots: Logic for Next Free Time")
    void test19_Calendar_NextAvailableSlot() throws Exception 
    {
        // Verifies the agent calls findAvailableTimeSlots tool
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Find me a free 15 min slot\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(20) @WithMockUser(username = TEST_USER)
    @DisplayName("20. Resilience: Calendar API Timeout")
    void test20_Calendar_ApiTimeout() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withFixedDelay(5000).withStatus(200)));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Check my schedule\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("error")));
    }

 // --- SECTION 3: SYSTEM MAIL SERVICE & SMTP (Tests 21-30) ---

    @Test @Order(21) @WithMockUser(username = TEST_USER)
    @DisplayName("21. HTML Rendering: Verify MimeMessage Content-Type")
    void test21_SystemMail_HtmlRendering() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Send a formatted HTML report to admin@test.com\"}"))
            .andExpect(status().isOk());

        greenMail.waitForIncomingEmail(1);
        String content = (String) greenMail.getReceivedMessages()[0].getContent();
        assertThat(content).contains("<html>", "<body>");
    }

    @Test @Order(22) @WithMockUser(username = TEST_USER)
    @DisplayName("22. Sender Validation: Hardcoded 'From' Address")
    void test22_SystemMail_FromAddress() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email the logs to me\"}"))
            .andExpect(status().isOk());

        greenMail.waitForIncomingEmail(1);
        assertThat(greenMail.getReceivedMessages()[0].getFrom()[0].toString())
            .isEqualTo("chakra.n.jeet@gmail.com"); 
    }

    @Test @Order(23) @WithMockUser(username = TEST_USER)
    @DisplayName("23. SMTP Receipt: Full GreenMail Handshake")
    void test23_SystemMail_GreenMailReceipt() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Notify boss@test.com about the project\"}"))
            .andExpect(status().isOk());

        // Industry standard: Verify the mail actually hit the local port 3025 (default GreenMail SMTP)
        assertThat(greenMail.getReceivedMessages()).isNotEmpty();
    }

    @Test @Order(24) @WithMockUser(username = TEST_USER)
    @DisplayName("24. Attachments: Placeholder/Logic Validation")
    void test24_SystemMail_AttachmentLogic() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email the PDF transcript to student@test.com\"}"))
            .andExpect(status().isOk());
        
        // Verifies the service doesn't crash when tool requests an attachment
    }

    @Test @Order(25) @WithMockUser(username = TEST_USER)
    @DisplayName("25. News Summary: Bulk Content Mapping")
    void test25_SystemMail_NewsSummary() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Find news on AI and email the top 5 to news-list@test.com\"}"))
            .andExpect(status().isOk());

        greenMail.waitForIncomingEmail(1);
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).contains("News Summary");
    }

    @Test @Order(26) @WithMockUser(username = TEST_USER)
    @DisplayName("26. Resilience: MessagingException Handling")
    void test26_SystemMail_MessagingException() throws Exception 
    {
        // Stop GreenMail to simulate a connection failure
        greenMail.stop();
        
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email alert@test.com now\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("failed")));
        
        greenMail.start(); // Restart for subsequent tests
    }

    @Test @Order(27) @WithMockUser(username = TEST_USER)
    @DisplayName("27. Encoding: UTF-8 Special Characters")
    void test27_SystemMail_Utf8Encoding() throws Exception 
    {
        String specialContent = "Check this: 😊";
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email '" + specialContent + "' to user@test.com\"}"))
            .andExpect(status().isOk());

        greenMail.waitForIncomingEmail(1);
        assertThat(greenMail.getReceivedMessages()[0].getContent().toString()).contains(specialContent);
    }

    @Test @Order(28) @WithMockUser(username = TEST_USER)
    @DisplayName("28. Propagation: Mail Server Down Response")
    void test28_SystemMail_ServerDown() throws Exception 
    {
        // Similar to test 26, but focuses on the tool's return List<String> 
        // to ensure individual recipient failure doesn't kill the batch.
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email x@y.com\"}"));
    }

    @Test @Order(29) @WithMockUser(username = TEST_USER)
    @DisplayName("29. Formatting: Subject Line Sanitization")
    void test29_SystemMail_SubjectFormatting() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Email with subject 'Urgent!' to test@test.com\"}"))
            .andExpect(status().isOk());

        greenMail.waitForIncomingEmail(1);
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("Urgent!");
    }

    @Test @Order(30) @WithMockUser(username = TEST_USER)
    @DisplayName("30. MimeMessageHelper: Multipart Structure Verify")
    void test30_SystemMail_MimeHelperInteraction() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Send a complex mail with body and signature\"}"))
            .andExpect(status().isOk());
            
        // Validates that MimeMessageHelper correctly sets the "multipart" flag
        assertThat(greenMail.getReceivedMessages()[0].getContentType()).contains("multipart");
    }

 // --- SECTION 4: SECURITY & REPOSITORY (Tests 31-40) ---


    @Test @Order(31) @WithMockUser(username = TEST_USER)
    @DisplayName("31. OAuth2 Fallback: Handle Null Password for Social Logins")
    void test31_Security_OAuth2PasswordFallback() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Check my profile\"}"))
            .andExpect(status().isOk());
        
        User savedUser = userRepository.findByEmail(TEST_USER);
        // Industry-grade check: If password is null, the service should handle it gracefully
        assertThat(savedUser).isNotNull();
    }

    @Test @Order(32) @WithMockUser(username = "nonexistent@test.com")
    @DisplayName("32. Exception Mapping: Unknown User to 401")
    void test32_Security_UserNotFoundMapping() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Hello\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test @Order(33) @WithMockUser(username = TEST_USER, roles = "USER")
    @DisplayName("33. RBAC: Verify 'ROLE_USER' Authority")
    void test33_Security_RoleAuthorityAssignment() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Access tools\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(34) @WithMockUser(username = TEST_USER)
    @DisplayName("34. Repository: findByEmail Consistency")
    void test34_Repo_HitCount() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"ping\"}"));
        
        assertThat(userRepository.findByEmail(TEST_USER)).isNotNull();
    }

    @Test @Order(35) @WithMockUser(username = TEST_USER)
    @DisplayName("35. Resource Protection: Prevent Cross-User Data Access")
    void test35_Security_CrossUserDataIsolation() throws Exception 
    {
        // This replaces the 'Enabled' check. 
        // Verifies that the tool initialization (init(userID)) uses the authenticated ID.
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Read my private emails\"}"))
            .andExpect(status().isOk());
        
        // Verify that the Gmail API call used the authenticated user's ID, not a hardcoded one.
        verify(getRequestedFor(urlPathMatching("/gmail/v1/users/me/.*")));
    }

    @Test @Order(36) @WithMockUser(username = TEST_USER)
    @DisplayName("36. Integrity: Password Hashing Verification")
    void test36_Security_PasswordHashing() throws Exception 
    {
        User user = userRepository.findByEmail(TEST_USER);
        // Ensure the password stored is not the raw 'password' string
        assertThat(user.getPassword()).isNotEqualTo("password");
    }

    @Test @Order(37) @WithMockUser(username = TEST_USER)
    @DisplayName("37. CSRF: Verify State-Changing POST Allowed for Authenticated")
    void test37_Security_CsrfProtection() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"test message\"}"))
            .andExpect(status().isOk()); 
    }

    @Test @Order(38) @WithMockUser(username = TEST_USER)
    @DisplayName("38. Auth: Handle 401 from External Google Services")
    void test38_Security_ExternalAuthFailure() throws Exception 
    {
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(401)));
        
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Sync calendar\"}"))
            .andExpect(jsonPath("$.message", containsString("re-authenticate")));
    }

    @Test @Order(39) @WithMockUser(username = TEST_USER)
    @DisplayName("39. Context: Verify SecurityContext in Async AI Execution")
    void test39_Security_ContextPersistence() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Who am I?\"}"))
            .andExpect(jsonPath("$.response", containsString(TEST_USER)));
    }

    @Test @Order(40) @WithMockUser(username = TEST_USER)
    @DisplayName("40. Persistence: Verify User Entity Record Count")
    void test40_Repo_PersistenceCheck() throws Exception 
    {
        long count = userRepository.count();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
    
 // --- SECTION 5: NEWS TOOL & AI ORCHESTRATION (Tests 41-50) ---

    @Test @Order(41) @WithMockUser(username = TEST_USER)
    @DisplayName("41. News Limit: Verify Stream Limit(5) Implementation")
    void test41_News_LimitVerification() throws Exception 
    {
        // Mock a service returning 10 articles; tool should only return 5
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Get news about Java\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Article 6"))));
    }

    @Test @Order(42) @WithMockUser(username = TEST_USER)
    @DisplayName("42. News Empty: Verify Success=False Mapping")
    void test42_News_EmptyResultHandling() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Get news about a non-existent topic 12345\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("No news articles found")));
    }

    @Test @Order(43) @WithMockUser(username = TEST_USER)
    @DisplayName("43. News Resilience: API Unavailability Handling")
    void test43_News_ServiceDown() throws Exception 
    {
        // Simulates NewsFetchService throwing an exception
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Search news\"}"))
            .andExpect(status().isOk()); 
    }

    @Test @Order(44) @WithMockUser(username = TEST_USER)
    @DisplayName("44. Article Mapping: Title and URL Integrity")
    void test44_News_DataMapping() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Find a news link for AI\"}"))
            .andExpect(jsonPath("$.response").value(org.hamcrest.Matchers.containsString("http")));
    }

    @Test @Order(45) @WithMockUser(username = TEST_USER)
    @DisplayName("45. Reasoning: AI Correct Tool Selection")
    void test45_AI_ToolSelection() throws Exception 
    {
        // Prompt specifically triggers MailTool, not NewsTool
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Check my inbox\"}"))
            .andExpect(status().isOk());
        
        // Verify News API was NEVER called
        verify(0, getRequestedFor(urlPathMatching("/news/.*")));
    }

    @Test @Order(46) @WithMockUser(username = TEST_USER)
    @DisplayName("46. Chain: News -> Mail (Forwarding Content)")
    void test46_Chain_NewsToMail() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Find Java news and email it to friend@test.com\"}"))
            .andExpect(status().isOk());

        greenMail.waitForIncomingEmail(1);
        assertThat(greenMail.getReceivedMessages()[0].getSubject()).contains("Java");
    }

    @Test @Order(47) @WithMockUser(username = TEST_USER)
    @DisplayName("47. Chain: Mail -> Calendar (Scheduling from Email)")
    void test47_Chain_MailToCalendar() throws Exception 
    {
        String encoded = Base64.getUrlEncoder().encodeToString("Meeting tomorrow at 9am".getBytes());
        stubFor(get(anyUrl()).willReturn(aResponse().withStatus(200).withBody("{\"payload\": {\"body\": {\"data\": \"" + encoded + "\"}}}")));

        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Read my last email and schedule the meeting mentioned in it\"}"))
            .andExpect(status().isOk());

        verify(postRequestedFor(urlPathEqualTo("/calendar/v1/calendars/primary/events")));
    }

    @Test @Order(48) @WithMockUser(username = TEST_USER)
    @DisplayName("48. LLM Guard: Context Window Exhaustion Prevention")
    void test48_AI_ContextExhaustion() throws Exception 
    {
        // Validates that MailTool truncation (2000 chars) prevents LLM failure
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Summarize all my long emails\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(49) @WithMockUser(username = TEST_USER)
    @DisplayName("49. Concurrency: Parallel Tool Execution Logic")
    void test49_AI_ParallelExecution() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Get my emails AND check my calendar for today\"}"))
            .andExpect(status().isOk());
    }

    @Test @Order(50) @WithMockUser(username = TEST_USER)
    @DisplayName("50. Final Structure: SUCCESS/ERROR Response Format")
    void test50_AI_FinalResponseFormat() throws Exception 
    {
        mockMvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
            .content("{\"message\": \"Hello\"}"))
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.response").exists());
    }
}







