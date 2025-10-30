package com.IntelliMate.core.tools;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import com.IntelliMate.core.service.CalendarService.GoogleCalendarService;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.TimePeriod;

import dev.langchain4j.agent.tool.Tool;




@Component
public class CalendarTool 
{
	private GoogleCalendarService googleCalendarService;
	
	
	
	public CalendarTool(GoogleCalendarService googleCalendarService)
	{
		this.googleCalendarService = googleCalendarService;
	}
	
	// Tool to check availability of users
	@Tool("Checks if a user or a list of users are available during a specified time range")
	private boolean checkAvailability(String userID, String startTime, String endTime, List<String> attendeeEmails)
	{
		try
		{
			Map<String, FreeBusyCalendar> calendars = googleCalendarService.checkFreeBusy(userID, attendeeEmails, startTime, endTime).getCalendars();
			
			for(String email : attendeeEmails) 
			{
				// Get the calendar for each attendee
			    FreeBusyCalendar calendar = calendars.get(email);
			     
			    // Check for busy times by checking if the busy list is not empty
			    if(calendar != null) 
			    {
			    	// Get busy times
			        List<TimePeriod> busyTimes = calendar.getBusy();
			            
			        // If the busy times list is not empty for any one of the attendees, slot not free return false
			        if(busyTimes != null && !busyTimes.isEmpty()) 
			        {			        	
			            return false; 
			        }
			    }
			}
			 
			// If no busy times found for any attendee, return true
			return true;
		}
		
		catch(IOException e)
		{
			e.printStackTrace();
		}
		
		return false;
	}
}





