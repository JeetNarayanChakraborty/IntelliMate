package com.IntelliMate.core.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.service.MailService.MailSendAndGetService;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartBody;
import com.google.api.services.gmail.model.MessagePartHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.*;



@ExtendWith(MockitoExtension.class)
class MailTool_Test 
{
    @Mock
    private MailSendAndGetService mailSendService;

    @InjectMocks
    private MailTool mailTool;

    private final String USER_ID = "tester@intellimate.com";

    @BeforeEach
    void setUp() 
    {
        mailTool.init(USER_ID);
    }

    @Test
    @DisplayName("GIVEN multiple recipients WHEN sending email THEN verify each service call")
    void sendEmail_ShouldAttemptAllRecipients() throws Exception 
    {
        // Arrange
        List<String> recipients = Arrays.asList("user1@test.com", "user2@test.com");
        String subject = "Meeting";
        String body = "Hello";
        
        when(mailSendService.sendMail(eq(USER_ID), anyString(), eq(subject), eq(body), any()))
            .thenReturn("SENT");

        // Act
        List<String> results = mailTool.sendEmail(recipients, subject, body, null);

        // Assert
        assertThat(results).hasSize(2).containsOnly("SENT");
        verify(mailSendService, times(2)).sendMail(eq(USER_ID), anyString(), eq(subject), eq(body), any());
    }

    @Test
    @DisplayName("GIVEN raw Gmail messages WHEN fetching THEN return cleaned and truncated metadata")
    void getMails_ShouldReturnCleanedMetadata() throws Exception 
    {
        // Arrange
        Message mockMsg = new Message()
                .setId("msg123")
                .setSnippet("Short snippet");

        // Setup Headers (From, Subject)
        MessagePartHeader fromHeader = new MessagePartHeader().setName("From").setValue("sender@test.com");
        MessagePartHeader subjectHeader = new MessagePartHeader().setName("Subject").setValue("Unit Test");
        
        // Setup Body (Base64 encoded "Hello World")
        String encodedBody = Base64.getUrlEncoder().encodeToString("Hello World".getBytes(StandardCharsets.UTF_8));
        MessagePart payload = new MessagePart()
                .setHeaders(Arrays.asList(fromHeader, subjectHeader))
                .setMimeType("text/plain")
                .setBody(new MessagePartBody().setData(encodedBody));

        mockMsg.setPayload(payload);

        when(mailSendService.getEmails(USER_ID, 1)).thenReturn(Collections.singletonList(mockMsg));

        // Act
        List<Map<String, String>> results = mailTool.getMails(1);

        // Assert
        assertThat(results).hasSize(1);
        Map<String, String> email = results.get(0);
        assertThat(email.get("from")).isEqualTo("sender@test.com");
        assertThat(email.get("subject")).isEqualTo("Unit Test");
        assertThat(email.get("body")).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("GIVEN a body exceeding 2000 characters WHEN fetching THEN verify truncation")
    void getMails_ShouldTruncateLongBodies() throws Exception 
    {
        // Arrange
        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < 2100; i++) longBody.append("A");
        
        String encoded = Base64.getUrlEncoder().encodeToString(longBody.toString().getBytes(StandardCharsets.UTF_8));
        Message mockMsg = new Message().setPayload(new MessagePart()
                .setMimeType("text/plain")
                .setBody(new MessagePartBody().setData(encoded)));

        when(mailSendService.getEmails(USER_ID, 1)).thenReturn(Collections.singletonList(mockMsg));

        // Act
        List<Map<String, String>> results = mailTool.getMails(1);

        // Assert
        String processedBody = results.get(0).get("body");
        assertThat(processedBody).hasSize(2027); 
        assertThat(processedBody).endsWith("... [Truncated for brevity]");
    }

    @Test
    @DisplayName("GIVEN a service failure WHEN fetching mails THEN return error map instead of throwing")
    void getMails_ShouldHandleExceptionsGracefully() throws Exception 
    {
        // Arrange
        when(mailSendService.getEmails(anyString(), anyInt())).thenThrow(new RuntimeException("API Down"));

        // Act
        List<Map<String, String>> results = mailTool.getMails(5);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).get("error")).contains("System failed to process mail: API Down");
    }
}






