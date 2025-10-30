package com.IntelliMate.core.tools;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.IntelliMate.core.service.CalendarService.GoogleCalendarService;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.FreeBusyCalendar;
import com.google.api.services.calendar.model.TimePeriod;
import com.google.api.services.calendar.model.Event;
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
	@Tool(name = "Checks if a user or a list of users are available during a specified time range",
			   value = "Checks if a user or a list of users are available during a specified time range. " +
	           "Returns true if all users are available, false if any user is busy. " +
	           "Use this when user asks about 'are they free', 'is everyone available', 'check availability', or 'schedule meeting'. "
	           + "or anything related to checking availability")
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
	
	// Tool to get today's events for a user
	@Tool(name = "Get all the calendar events for a user on current day",
		 	   value = "Retrieves all calendar events for the current day. " +
	           "Returns a list of event objects with properties: id (string), summary (string), " +
	           "description (string), start time (ISO 8601 datetime), end time (ISO 8601 datetime), " +
	           "location (string), attendees (list of email strings), status (string). " +
	           "Use this when user asks about 'today', 'today's schedule', 'what's on my calendar today', or 'today's meetings'. "
	           + "or anything related to the current day" + "Returns empty list if no events found.")
	private List<Event> getTodaysEvents(String userID, String date)
	{
		try
		{
			return googleCalendarService.getEventsForDay(userID, date);
		}
		
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	// Tool to get this week's events for a user
	@Tool(name = "Get weekly events for the current week",
			   value = "Retrieves all calendar events for the current week (Monday to Sunday). " +
	           "Returns a list of event objects with properties: id (string), summary (string), " +
	           "description (string), start time (ISO 8601 datetime), end time (ISO 8601 datetime), " +
	           "location (string), attendees (list of email strings), status (string). " +
	           "Use this when user asks about 'this week', 'weekly schedule', 'what's coming up', or 'week's meetings'. "
	           + "or anything related to the current week" + "Returns empty list if no events found.")
	private List<Event> getWeeksEvents(String userID, String weekStartDate, String weekEndDate)
	{
		try
		{
			return googleCalendarService.getEventsForWeek(userID);
		}
		
		catch(IOException e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	// Tool to find available time slots for a user or a list of users
	@Tool(name = "Find available time slots for a user or a list of users within the specified duration in minutes",
			   value = "Finds available time slots for a user or a list of users within the specified duration in minutes. " +
	           "Returns a list of available time slots as ISO 8601 datetime objects. " +
	           "Use this when user asks about 'find me a time', 'available slots', 'schedule a meeting', or 'free time'. "
	           + "or anything related to finding free time slots")
	private List<DateTime> findAvailableTimeSlots(String userID, List<String> attendeeEmails, int slotDurationInMinutes)
	{
		return googleCalendarService.getNextAvailableSlots(userID, attendeeEmails, slotDurationInMinutes);
	}
}














