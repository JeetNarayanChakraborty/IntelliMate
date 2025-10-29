package com.IntelliMate.core.service.CalendarService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.stereotype.Service;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;




@Service
public class GoogleCalendarService 
{
	private final GoogleOAuthService googleOAuthService;


	
	public GoogleCalendarService(GoogleOAuthService googleOAuthService) 
	{
		this.googleOAuthService = googleOAuthService;
	}
	
	private Calendar getGoogleCalenderService(String userId) throws IOException
	{
		Credential credential = googleOAuthService.getStoredCredential(userId);
		
		if(credential == null) 
        {
            throw new IllegalStateException("User not authenticated");
        }
        
		try
		{
			return new Calendar.Builder(
		            GoogleNetHttpTransport.newTrustedTransport(),
		            GsonFactory.getDefaultInstance(),
		            credential
		        ).setApplicationName("IntelliMate").build();
		}
		
		catch(GeneralSecurityException e) 
		{
			throw new IOException("Failed to create Calendar service", e);
		}
	}
	
	// Get all calendar events for a specific day for a user
	public List<Event> getEventsForDay(String userId, String date) throws IOException 
	{
	    Calendar service = getGoogleCalenderService(userId);
	    
	    try 
	    {
	        // Parse the date string (format: "2025-10-30")
	        LocalDate localDate = LocalDate.parse(date);
	        
	        // Start of the day (00:00:00)
	        LocalDateTime startOfDay = localDate.atStartOfDay();
	        DateTime timeMin = new DateTime(Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()));
	        
	        // End of the day (23:59:59)
	        LocalDateTime endOfDay = localDate.atTime(23, 59, 59);
	        DateTime timeMax = new DateTime(Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));
	        
	        // Fetch events for that day
	        Events events = service.events().list("primary")
	            .setTimeMin(timeMin)
	            .setTimeMax(timeMax)
	            .setOrderBy("startTime")
	            .setSingleEvents(true)
	            .execute();
	        
	        List<Event> eventsList = events.getItems();
	        
	        return eventsList != null ? eventsList : new ArrayList<>();
	    } 
	    
	    catch(Exception e) 
	    {
	        throw new IOException("Failed to fetch events for the day: " + e.getMessage(), e);
	    }
	}
	
	
	// Get all calendar events for a specific day for a user
	public List<Event> getEventsForWeek(String userId, String date) throws IOException 
	{
		Calendar service = getGoogleCalenderService(userId);
		    
		try 
		{		        
		    // Monday start of the day (00:00:00)
		    LocalDateTime mondayStartOfDay = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
		    								 .atStartOfDay();
		    // Convert to DateTime
		    DateTime timeMin = new DateTime(Date.from(mondayStartOfDay.atZone(ZoneId.systemDefault()).toInstant()));
		        
		    // Sunday End of the day (23:59:59)
		    LocalDateTime sundayEndOfDay = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
		    							   .atTime(23, 59, 59);
		    // Convert to DateTime
		    DateTime timeMax = new DateTime(Date.from(sundayEndOfDay.atZone(ZoneId.systemDefault()).toInstant()));
		        
		    // Fetch events for current week
		    Events events = service.events().list("primary")
		        .setTimeMin(timeMin)
		        .setTimeMax(timeMax)
		        .setOrderBy("startTime")
		        .setSingleEvents(true)
		        .execute();
		        
		    List<Event> eventsList = events.getItems();
		        
		    return eventsList != null ? eventsList : new ArrayList<>();
		} 
		    
		catch(Exception e) 
		{
		    throw new IOException("Failed to fetch events for the day: " + e.getMessage(), e);
		}
	}
	
	
	
	
	
	
	
	
	
	
	
	

}
















