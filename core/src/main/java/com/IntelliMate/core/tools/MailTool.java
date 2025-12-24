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
	
	@Tool(name = "get_mails", value = "Gets emails from the user's inbox")
	public List<Message> getMails(int maxResults)
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







