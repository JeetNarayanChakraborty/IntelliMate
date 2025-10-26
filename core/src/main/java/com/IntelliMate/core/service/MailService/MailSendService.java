package com.IntelliMate.core.service.MailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Properties;



@Service
public class MailSendService
{
    @Autowired
    private GoogleOAuthService googleOAuthService;

    public String sendMail(String userID, String to, String subject, String body) 
        throws IOException, MessagingException 
    {
        Gmail service = getGmailService(userID);
        
        // Create email using Jakarta Mail
        MimeMessage email = createEmail(to, "me", subject, body);
        
        // Convert MimeMessage to Gmail Message
        Message message = createMessageWithEmail(email);
        
        // Send email
        message = service.users().messages().send("me", message).execute();
        
        return "Email sent successfully with ID: " + message.getId();
    }
    
    // Create email using Jakarta Mail
    private MimeMessage createEmail(String to, String from, String subject, String bodyText) 
        throws MessagingException 
    {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        
        return email;
    }
    
    // Convert MimeMessage to Gmail Message
    private Message createMessageWithEmail(MimeMessage emailContent) 
        throws MessagingException, IOException 
    {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        
        Message message = new Message();
        message.setRaw(encodedEmail);
        
        return message;
    }
    
    // Helper method to get Gmail service
    private Gmail getGmailService(String userId) throws IOException 
    {
        Credential credential = googleOAuthService.getStoredCredential(userId);
        
        if(credential == null) throw new IllegalStateException("User not authenticated");
 
        try 
        {
            return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("IntelliMate").build();
        } 
        
        catch(GeneralSecurityException e) {throw new IOException("Failed to create Gmail service", e);}
    }
}





