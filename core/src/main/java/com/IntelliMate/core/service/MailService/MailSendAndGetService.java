package com.IntelliMate.core.service.MailService;

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
import com.google.api.services.gmail.model.ListMessagesResponse;
import java.util.List;
import java.util.ArrayList;
import jakarta.mail.MessagingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Properties;



@Service
public class MailSendAndGetService
{
    private final GoogleOAuthService googleOAuthService;
    
    
    
    public MailSendAndGetService(GoogleOAuthService googleOAuthService) 
	{
		this.googleOAuthService = googleOAuthService;
	}
    
    
    // Send email method
    public String sendMail(String userID, String to, String subject, String body, String threadID) 
        throws IOException, MessagingException 
    {
    	try
    	{
    		Gmail service = getGmailService(userID);
    		String originalMessageID = null;
    		
    		// If it's a reply, fetch the original message-id header
            if (threadID != null && !threadID.isEmpty()) 
            {
                Message original = service.users().messages().get("me", threadID).execute();
                originalMessageID = original.getPayload().getHeaders().stream()
                					.filter(h -> h.getName().equalsIgnoreCase("Message-ID"))
                					.findFirst()
                					.map(h -> h.getValue())
                					.orElse(null);
            }
    		
            // Create email using Jakarta Mail
            MimeMessage email = createEmail(to, "me", subject, body, originalMessageID);
            
            // Convert MimeMessage to Gmail Message
            Message message = createMessageWithEmail(email);
            
            // Send email
            message = service.users().messages().send("me", message).execute();
            
            return "Email sent successfully with ID: " + message.getId() + " to " + to;
    	}
    	
    	catch(Exception e)
		{
			return "Failed to send email: " + e.getMessage();
		}
    }
    
    
    // Retrieve emails method
    public List<Message> getEmails(String userId, int maxResults) throws IOException 
    {
    	// Get Gmail service
    	Gmail service = getGmailService(userId);
    	
    	// Set up request to list messages
    	ListMessagesResponse response = service.users().messages()
    	    .list("me")
    	    .setMaxResults((long) maxResults)
    	    .execute();
    	    
    	List<Message> fullMessages = new ArrayList<>();
    	
    	// Fetch full message details for each message
    	for(Message message : response.getMessages()) 
    	{
    		// Get full message details
    	    Message fullMessage = service.users().messages().get("me", message.getId()).execute();
    	    
    	    // Add to the list
    	    fullMessages.add(fullMessage);
    	}
    	    
    	return fullMessages; 
    } 
    
    
    // Create email using Jakarta Mail
    private MimeMessage createEmail(String to, String from, String subject, String bodyText, String originalMessageID) 
        throws MessagingException 
    {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        
        // If originalMessageID exists, set the reply headers
        if(originalMessageID != null) 
        {
            email.setHeader("In-Reply-To", originalMessageID);
            email.setHeader("References", originalMessageID);
        }
        
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





