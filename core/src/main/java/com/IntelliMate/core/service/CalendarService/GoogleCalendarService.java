package com.IntelliMate.core.service.CalendarService;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.springframework.stereotype.Service;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
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
import java.util.UUID;
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
		// Current day to ensure the loop stops at midnight
	    int startDay = ZonedDateTime.now().getDayOfMonth();
		
		// Get start time (current time)
		String startTime = ZonedDateTime.now().
				           format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		
		
		
		
		
		
		
		
		ZonedDateTime now = ZonedDateTime.now();
				System.out.println("DEBUG: The Java Server thinks today is: " + now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

		
		
		
		
		
		
		
		
		

		// Get end time (current time + duration in minutes)
		String endTime = ZonedDateTime.now().plusMinutes(slotDurationInMinutes).
				                             format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		
		List<DateTime> availableSlots = new ArrayList<>();
		boolean slotIsBusy = false;
		
		// Loop and find all available slots until the end of the day
		while(ZonedDateTime.parse(endTime).getDayOfMonth() == startDay)
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
				        	slotIsBusy = true;
				            break;
				        }
				        
				        else if(busyTimes == null || busyTimes.isEmpty())
				        {
				        	slotIsBusy = false;
				        	break;
				        }
				    }
				}
				
				if(!slotIsBusy)
				{
					// If no busy times found for any attendee, add to available slots
					availableSlots.add(DateTime.parseRfc3339(startTime));
				}
				
				// Move to the next slot
				startTime = endTime;
	        	endTime = ZonedDateTime.parse(endTime).plusMinutes(slotDurationInMinutes).
	        			 				format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			}
			
			catch(IOException e)
			{
				e.printStackTrace();
				
				// prevent infinite loop in case of any error like network issues etc.
				startTime = endTime;
	        	endTime = ZonedDateTime.parse(endTime).plusMinutes(slotDurationInMinutes).
	        			 				format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			}	
		}
		
		return availableSlots;
	}
	
	// Schedule a meeting for multiple attendees at the next available slot
	public String scheduleMeetingWithAttendees(String userID, List<String> attendeeEmails, String title, 
			                      String description, String availableSlot, int durationInMinutes) throws IOException 
			    
	{
	    // Take the available slot
	    DateTime startDateTime = new DateTime(availableSlot);
	    
	    // Calculate the end time based on duration
	    ZonedDateTime startZoned = ZonedDateTime.parse(startDateTime.toStringRfc3339());
	    ZonedDateTime endZoned = startZoned.plusMinutes(durationInMinutes);
	    DateTime endDateTime = new DateTime(Date.from(endZoned.toInstant()));

	    // Initialize the Calendar service
	    Calendar service = getGoogleCalenderService(userID);

	    // Build the Event object
	    Event event = new Event()
	        .setSummary(title)
	        .setDescription(description);

	    // Set Start Time
	    EventDateTime start = new EventDateTime()
	        .setDateTime(startDateTime)
	        .setTimeZone(ZoneId.systemDefault().getId());
	    event.setStart(start);

	    // Set End Time
	    EventDateTime end = new EventDateTime()
	        .setDateTime(endDateTime)
	        .setTimeZone(ZoneId.systemDefault().getId());
	    event.setEnd(end);

	    // Add Attendees
	    List<EventAttendee> attendees = new ArrayList<>();
	    
	    for(String email : attendeeEmails) 
	    {
	        attendees.add(new EventAttendee().setEmail(email));
	    }
	    
	    event.setAttendees(attendees);
	    
	    // Add Google Meet conference data
	    ConferenceSolutionKey solutionKey = new ConferenceSolutionKey().setType("hangoutsMeet");
	    
	    // Generate a unique request ID for the conference
	    CreateConferenceRequest createRequest = new CreateConferenceRequest()
	            .setRequestId(UUID.randomUUID().toString()) 
	            .setConferenceSolutionKey(solutionKey);
	    
	    ConferenceData conferenceData = new ConferenceData().setCreateRequest(createRequest);
	    
	    // Attach the conference data to the event
	    event.setConferenceData(conferenceData);
	    

	    try 
	    {
	        // Insert the event and send email invites
	        event = service.events().insert("primary", event)
	                .setSendUpdates("all")        // setSendUpdates("all") triggers the email notifications to attendees
	                .setConferenceDataVersion(1)  // Required to trigger Meet link generation
	                .execute();

	        return "Success! Meeting '" + title + "' scheduled for " + startZoned.toLocalTime() + 
	               ". Invites have been sent to: " + String.join(", ", attendeeEmails);
	    } 
	    
	    catch(IOException e) 
	    {
	        return "Error while creating calendar event: " + e.getMessage();
	    }
	}
	
	//updates an event by user ID and event ID
	public String updateMeeting(String userID, String eventId, String newTitle, String newSlot, Integer newDuration) 
	{
	    try 
	    {
	        Calendar service = getGoogleCalenderService(userID);

	        // Fetch the existing event first
	        Event event = service.events().get("primary", eventId).execute();

	        // Update Title if provided
	        if(newTitle != null && !newTitle.isEmpty()) 
	        {
	            event.setSummary(newTitle);
	        }

	        // Update Time/Duration if a new slot is provided
	        if(newSlot != null && !newSlot.isEmpty()) 
	        {
	            DateTime startDateTime = new DateTime(newSlot);
	            event.setStart(new EventDateTime().setDateTime(startDateTime).setTimeZone("Asia/Kolkata"));

	            // Calculate new end time
	            int duration = (newDuration != null) ? newDuration : 30; // defaults to 30 if not specified
	            ZonedDateTime startZoned = ZonedDateTime.parse(startDateTime.toStringRfc3339());
	            ZonedDateTime endZoned = startZoned.plusMinutes(duration);
	            DateTime endDateTime = new DateTime(Date.from(endZoned.toInstant()));
	            
	            event.setEnd(new EventDateTime().setDateTime(endDateTime).setTimeZone("Asia/Kolkata"));
	        }

	        // Push the update back to Google
	        event = service.events().update("primary", eventId, event)
	                .setSendUpdates("all")       // Notifies attendees of the change
	                .setConferenceDataVersion(1) // Preserves the Google Meet link
	                .execute();

	        return "Success! Meeting '" + event.getSummary() + "' has been updated. Attendees have been notified.";

	    } 
	    
	    catch(IOException e) 
	    {
	        return "Error updating event: " + e.getMessage();
	    }
	}
	
	// Delete an event by event ID
	public String deleteCalendarEvent(String userID, String eventId) 
	{
	    try 
	    {
	        // Initialize the Calendar service
	        Calendar service = getGoogleCalenderService(userID);

	        // Delete the event
	        service.events().delete("primary", eventId) // Uses "primary" for the user's main calendar
	                .setSendUpdates("all") // Notifies attendees that the meeting is cancelled
	                .execute();

	        return "Success! Event with ID '" + eventId + "' has been deleted and attendees notified.";
	    } 
	    
	    catch(IOException e) 
	    {
	        return "Error while deleting calendar event: " + e.getMessage();
	    }
	}	
}















