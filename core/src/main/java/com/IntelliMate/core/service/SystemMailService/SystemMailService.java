package com.IntelliMate.core.service.SystemMailService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;



@Service
public class SystemMailService 
{
    @Autowired
    private JavaMailSender mailSender;
    

    public void sendEmail(String to, String subject, String body) 
	{
        SimpleMailMessage message = new SimpleMailMessage();
        
        message.setFrom("chakra.n.jeet@gmail.com"); 
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
        
        System.out.println("SMTP Mail sent successfully to " + to);
    }
    
    public void sendEmailWithHTML(String to, String subject, String body) throws MessagingException 
    {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setFrom("chakra.n.jeet@gmail.com");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); 

        mailSender.send(mimeMessage);
    }
}





