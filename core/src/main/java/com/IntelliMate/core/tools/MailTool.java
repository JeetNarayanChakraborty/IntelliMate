package com.IntelliMate.core.tools;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.IntelliMate.core.service.MailService.MailSendAndGetService;
import dev.langchain4j.agent.tool.Tool;
import com.google.api.services.gmail.model.Message;
import java.util.Base64;
import com.google.api.services.gmail.model.MessagePart;
import java.nio.charset.StandardCharsets;
import java.util.*;



@Component
public class MailTool 
{
	private MailSendAndGetService mailSendService;
	private String userID;
	
	
	public MailTool(MailSendAndGetService mailSendService)
	{
		this.mailSendService = mailSendService;
	}
	
	public void init(String userID)
	{
		this.userID = userID;
	}

	@Tool(name = "send_mail", value = "Sends an email to specified recipients")
	public List<String> sendEmail(List<String> to, String subject, String body, String threadID)
	{
		List<String> emails = new ArrayList<>();
		
		for(String recipient : to)
		{
			try 
			{
				// Send email using MailSendService
				String result = mailSendService.sendMail(userID, recipient, subject, body, threadID);
				emails.add(result);
			} 
			
			catch(Exception e) 
			{
				// Continue on exception to attempt sending to other recipients
				continue;
			}
		}
		
		return emails;
	}
	

	@Tool(name = "get_mails", value = "Fetches and cleans the latest emails. Returns Subject, From, Date, and Decoded Body content.")
	public List<Map<String, String>> getMails(int maxResults) 
	{
	    try 
	    {
	        // Fetch raw messages from your service
	        List<Message> rawMessages = mailSendService.getEmails(userID, maxResults);
	        List<Map<String, String>> cleanMessages = new ArrayList<>();

	        for(Message msg : rawMessages) 
	        {
	            Map<String, String> emailMap = new HashMap<>();
	            
	            // Extract Basic Metadata
	            emailMap.put("id", msg.getId());
	            emailMap.put("snippet", msg.getSnippet());
	            
	            // Extract Headers (From, Subject, Date)
	            if(msg.getPayload() != null && msg.getPayload().getHeaders() != null) 
	            {
	                for(var header : msg.getPayload().getHeaders()) 
	                {
	                    String name = header.getName().toLowerCase();
	                    if(name.equals("from")) emailMap.put("from", header.getValue());
	                    if(name.equals("subject")) emailMap.put("subject", header.getValue());
	                    if(name.equals("date")) emailMap.put("date", header.getValue());
	                }
	            }

	            // Recursive Body Extraction (Deep Cleaning)
	            StringBuilder mailBodyBuilder = new StringBuilder();
	            extractTextContent(msg.getPayload(), mailBodyBuilder);
	            
	            // Context Management (Truncation)
	            // Truncate to ~2000 chars per mail so 10 mails don't crash the LLM context
	            String body = mailBodyBuilder.toString();
	            
	            if(body.length() > 2000) 
	            {
	                body = body.substring(0, 2000) + "... [Truncated for brevity]";
	            }
	            
	            emailMap.put("body", body.isEmpty() ? "No text content available" : body);

	            cleanMessages.add(emailMap);
	        }
	        
	        return cleanMessages;
	    } 
	    
	    catch(Exception e) 
	    {
	        // Return the error as a string so the AI can report it to you
	        return List.of(Map.of("error", "System failed to process mail: " + e.getMessage()));
	    }
	}
	
	private void extractTextContent(MessagePart part, StringBuilder builder) 
	{
	    if(part == null) return;

	    // We prioritize text/plain for LLMs as it's the most token-efficient
	    if(part.getMimeType().equals("text/plain") && part.getBody().getData() != null) 
	    {
	        String encodedData = part.getBody().getData();
	        byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedData);
	        builder.append(new String(decodedBytes, StandardCharsets.UTF_8));
	    } 
	    
	    // If it's a container part (multipart), recurse into its children
	    else if(part.getParts() != null) 
	    {
	        for(MessagePart subPart : part.getParts()) 
	        {
	            extractTextContent(subPart, builder);
	            
	            // If already found plain text, can stop 
	            if(builder.length() > 0) break; 
	        }
	    }
	}
}







