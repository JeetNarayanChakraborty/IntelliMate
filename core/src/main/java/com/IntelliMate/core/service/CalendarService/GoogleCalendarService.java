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
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.FreeBusyRequest;
import com.google.api.services.calendar.model.FreeBusyRequestItem;
import com.google.api.services.calendar.model.FreeBusyResponse;
import com.google.api.services.calendar.model.TimePeriod;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.util.DateTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
	
	
	// Get all calendar events for current week for a user
	public List<Event> getEventsForWeek(String userId) throws IOException 
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
		    LocalDateTime sundayEndOfDay = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
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
	
	// Check free/busy status for multiple attendees within a time range
	public FreeBusyResponse checkFreeBusy(String userID, List<String> attendeeEmails, 
            String startDateTime, String endDateTime) throws IOException
	{
		// Initialize Calendar service
	    Calendar service = getGoogleCalenderService(userID);
	    
	    try 
	    {
	        // Directly use the formatted strings from LLM
	        DateTime timeMin = DateTime.parseRfc3339(startDateTime);
	        DateTime timeMax = DateTime.parseRfc3339(endDateTime);
	        
	        // Create FreeBusyRequestItem for each attendee email
	        List<FreeBusyRequestItem> items = new ArrayList<>();
	        for (String email : attendeeEmails) 
	        {
	            FreeBusyRequestItem item = new FreeBusyRequestItem();
	            item.setId(email);
	            items.add(item);
	        }
	        
	        // Build the FreeBusy request
	        FreeBusyRequest request = new FreeBusyRequest();
	        request.setTimeMin(timeMin); // Set start time
	        request.setTimeMax(timeMax); // Set end time
	        request.setItems(items); // Set attendees
	        
	        // Execute the query
	        FreeBusyResponse response = service.freebusy().query(request).execute();
	        
	        return response;
	    } 
	    
	    catch(Exception e) 
	    {
	        throw new IOException("Failed to check free/busy status: " + e.getMessage(), e);
	    }
	}
	
	// Get next available time slots for multiple attendees
	public List<DateTime> getNextAvailableSlots(String userID, List<String> attendeeEmails, int slotDurationInMinutes)
	{	
		// Get start time (current time)
		String startTime = ZonedDateTime.now().
				           format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		// Get end time (current time + duration in minutes)
		String endTime = ZonedDateTime.now().plusMinutes(slotDurationInMinutes).
				                             format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		
		List<DateTime> availableSlots = new ArrayList<>();
		
		// Loop and find all available slots until the end of the day
		while(ZonedDateTime.parse(endTime).toLocalTime().getHour() <= 23 && 
			  ZonedDateTime.parse(endTime).toLocalTime().getMinute() <= 59)
		{
			try
			{
				Map<String, FreeBusyCalendar> calendars = checkFreeBusy(userID, attendeeEmails, startTime, endTime).getCalendars();
				
				for(String email : attendeeEmails) 
				{
					// Get the calendar for each attendee
				    FreeBusyCalendar calendar = calendars.get(email);
				     
				    // Check for busy times by checking if the busy list is not empty
				    if(calendar != null) 
				    {
				    	// Get busy times
				        List<TimePeriod> busyTimes = calendar.getBusy();
				            
				        // If the busy times list is not empty for any one of the attendees, move to the next slot
				        if(busyTimes != null && !busyTimes.isEmpty()) 
				        {
				        	startTime = endTime;
				        	endTime = ZonedDateTime.parse(endTime).plusMinutes(slotDurationInMinutes).
				        			 				format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
				        	
				            continue; // Check next slot
				        }
				    }
				}
				 
				// If no busy times found for any attendee, add to available slots
				availableSlots.add(DateTime.parseRfc3339(startTime));
			}
			
			catch(IOException e)
			{
				e.printStackTrace();
			}	
		}
		
		return availableSlots;
	}
}































