package com.IntelliMate.core.service.MailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.Collections;
import java.util.List;




@ExtendWith(MockitoExtension.class)
class MailSendAndGetService_Test 
{
    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private Gmail gmailClient;

    @Mock
    private Gmail.Users users;

    @Mock
    private Gmail.Users.Messages messages;

    @Spy
    @InjectMocks
    private MailSendAndGetService mailService;

    private final String USER_ID = "user-001";
    private final String RECIPIENT = "target@example.com";

    
    
    
    @BeforeEach
    void setUp() throws IOException 
    {
        // Stubbing the internal service creator to return our mock client
        lenient().doReturn(gmailClient).when(mailService).getGmailService(USER_ID);
        
        // Setup the common mock chain for Gmail SDK: service.users().messages()
        lenient().when(gmailClient.users()).thenReturn(users);
        lenient().when(users.messages()).thenReturn(messages);
    }

    @Test
    @DisplayName("GIVEN valid details WHEN sending email THEN return success message with ID")
    void sendMail_ShouldReturnSuccess() throws IOException, MessagingException 
    {
        // Arrange
        Message mockSentMessage = new Message().setId("msg_123");
        Gmail.Users.Messages.Send sendRequest = mock(Gmail.Users.Messages.Send.class);

        when(messages.send(eq("me"), any(Message.class))).thenReturn(sendRequest);
        when(sendRequest.execute()).thenReturn(mockSentMessage);

        // Act
        String result = mailService.sendMail(USER_ID, RECIPIENT, "Subject", "Body", null);

        // Assert
        assertThat(result).contains("Email sent successfully", "msg_123", RECIPIENT);
        verify(messages).send(eq("me"), argThat(msg -> msg.getRaw() != null));
    }

    @Test
    @DisplayName("GIVEN a threadID WHEN replying THEN fetch original Message-ID for headers")
    void sendMail_WithThreadId_ShouldSetReplyHeaders() throws IOException, MessagingException 
    {
        // Arrange
        String threadId = "thread_abc";
        Message originalMessage = new Message().setPayload(new MessagePart().setHeaders(
                List.of(new MessagePartHeader().setName("Message-ID").setValue("<original@msg.id>"))
        ));

        Gmail.Users.Messages.Get getRequest = mock(Gmail.Users.Messages.Get.class);
        Gmail.Users.Messages.Send sendRequest = mock(Gmail.Users.Messages.Send.class);

        when(messages.get("me", threadId)).thenReturn(getRequest);
        when(getRequest.execute()).thenReturn(originalMessage);
        when(messages.send(eq("me"), any(Message.class))).thenReturn(sendRequest);
        when(sendRequest.execute()).thenReturn(new Message().setId("reply_123"));

        // Act
        mailService.sendMail(USER_ID, RECIPIENT, "Re: Subject", "Reply Body", threadId);

        // Assert
        verify(messages).get("me", threadId);
        verify(messages).send(eq("me"), any(Message.class));
    }

    @Test
    @DisplayName("GIVEN max results WHEN fetching emails THEN return list of full message details")
    void getEmails_ShouldReturnFullMessageDetails() throws IOException 
    {
        // Arrange
        int maxResults = 2;
        Message summaryMsg = new Message().setId("id_1");
        ListMessagesResponse listResponse = new ListMessagesResponse()
                .setMessages(Collections.singletonList(summaryMsg));

        Gmail.Users.Messages.List listRequest = mock(Gmail.Users.Messages.List.class);
        Gmail.Users.Messages.Get getRequest = mock(Gmail.Users.Messages.Get.class);

        when(messages.list("me")).thenReturn(listRequest);
        when(listRequest.setMaxResults((long) maxResults)).thenReturn(listRequest);
        when(listRequest.execute()).thenReturn(listResponse);
        
        when(messages.get("me", "id_1")).thenReturn(getRequest);
        when(getRequest.execute()).thenReturn(new Message().setSnippet("Full Snippet"));

        // Act
        List<Message> result = mailService.getEmails(USER_ID, maxResults);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSnippet()).isEqualTo("Full Snippet");
        verify(messages).list("me");
        verify(messages).get("me", "id_1");
    }

    @Test
    @DisplayName("GIVEN an API error WHEN sending email THEN return failure message")
    void sendMail_ShouldHandleException() throws IOException 
    {
        // Arrange
        when(messages.send(anyString(), any(Message.class))).thenThrow(new IOException("API Error"));

        // Act
        String result="";
        
		try 
		{
			result = mailService.sendMail(USER_ID, RECIPIENT, "Sub", "Body", null);
		} 
		
		catch(Exception e) {}

        // Assert
        assertThat(result).contains("Failed to send email", "API Error");
    }
}







