package com.IntelliMate.core.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import com.IntelliMate.core.service.MailService.MailSendAndGetService;
import dev.langchain4j.agent.tool.Tool;
import com.google.api.services.gmail.model.Message;



@Component
public class MailTool 
{
	private MailSendAndGetService mailSendService;
	
	
	public MailTool(MailSendAndGetService mailSendService)
	{
		this.mailSendService = mailSendService;
	}

	@Tool("Sends an email to specified recipients")
	public List<String> sendEmail(String userID, List<String> to, String subject, String body)
	{
		List<String> emails = new ArrayList<>();
		
		for(String recipient : to)
		{
			try 
			{
				// Send email using MailSendService
				String result = mailSendService.sendMail(userID, recipient, subject, body);
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
	
	@Tool("Gets emails from the user's inbox")
	public List<Message> getMails(String userID, int maxResults)
	{
		try 
		{
			// Retrieve emails using MailSendService
			return mailSendService.getEmails(userID, maxResults);
		} 
		
		catch(IOException e) 
		{
			return new ArrayList<>();
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	
	

}
