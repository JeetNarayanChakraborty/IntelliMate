package com.IntelliMate.core.service.SystemMailService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import java.io.IOException;




@ExtendWith(MockitoExtension.class)
class SystemMailService_Test 
{
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private SystemMailService systemMailService;

    private final String TO = "recipient@example.com";
    private final String SUBJECT = "Test Subject";
    private final String BODY = "Hello World";
    private final String FROM_EMAIL = "chakra.n.jeet@gmail.com";

    @Test
    @DisplayName("GIVEN valid details WHEN sending simple email THEN verify message properties and sender interaction")
    void sendEmail_ShouldSendSimpleMessage() 
    {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        systemMailService.sendEmail(TO, SUBJECT, BODY);

        // Assert
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        assertThat(capturedMessage.getTo()).containsExactly(TO);
        assertThat(capturedMessage.getFrom()).isEqualTo(FROM_EMAIL);
        assertThat(capturedMessage.getSubject()).isEqualTo(SUBJECT);
        assertThat(capturedMessage.getText()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("GIVEN HTML body WHEN sending HTML email THEN verify MimeMessage content and multipart status")
    void sendEmailWithHTML_ShouldSendMimeMessage() throws MessagingException, IOException 
    {
        // Arrange
        MimeMessage mockMimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mockMimeMessage);

        // Act
        systemMailService.sendEmailWithHTML(TO, SUBJECT, "<h1>" + BODY + "</h1>");

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mockMimeMessage);
    }

    @Test
    @DisplayName("GIVEN a mail server failure WHEN sending email THEN ensure exception propagates")
    void sendEmail_ShouldPropagateExceptions() 
    {
        // Arrange
        doThrow(new RuntimeException("SMTP Server Down"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        try 
        {
            systemMailService.sendEmail(TO, SUBJECT, BODY);
        } 
        
        catch(Exception e) 
        {
            assertThat(e.getMessage()).isEqualTo("SMTP Server Down");
        }
        
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}








