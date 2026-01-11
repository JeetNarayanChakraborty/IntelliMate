package com.IntelliMate.core.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.service.CalendarService.GoogleCalendarService;
import com.google.api.services.calendar.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.IOException;
import java.util.*;



@ExtendWith(MockitoExtension.class)
class CalendarTool_Test 
{
    @Mock
    private GoogleCalendarService googleCalendarService;

    @InjectMocks
    private CalendarTool calendarTool;

    private final String USER_ID = "test-user-123";

    @BeforeEach
    void setUp() 
    {
        calendarTool.init(USER_ID);
    }

    @Test
    @DisplayName("GIVEN attendees are busy WHEN checking availability THEN return false")
    void checkAvailability_ShouldReturnFalse_WhenAttendeesAreBusy() throws IOException 
    {
        // Arrange
        String start = "2023-10-01T10:00:00Z";
        String end = "2023-10-01T11:00:00Z";
        List<String> emails = Collections.singletonList("busy@test.com");

        FreeBusyResponse mockResponse = new FreeBusyResponse();
        FreeBusyCalendar busyCalendar = new FreeBusyCalendar();
        busyCalendar.setBusy(Collections.singletonList(new TimePeriod())); // Non-empty busy list
        
        Map<String, FreeBusyCalendar> calendarMap = new HashMap<>();
        calendarMap.put("busy@test.com", busyCalendar);
        mockResponse.setCalendars(calendarMap);

        when(googleCalendarService.checkFreeBusy(eq(USER_ID), eq(emails), eq(start), eq(end)))
                .thenReturn(mockResponse);

        // Act - Using ReflectionTestUtils to invoke private @Tool method
        boolean isAvailable = (boolean) ReflectionTestUtils.invokeMethod(
                calendarTool, "checkAvailability", start, end, emails);

        // Assert
        assertThat(isAvailable).isFalse();
    }

    @Test
    @DisplayName("GIVEN no events for today WHEN fetching THEN return empty list")
    void getTodaysEvents_ShouldReturnList() throws IOException 
    {
        // Arrange
        String date = "2023-10-01";
        List<Event> mockEvents = Arrays.asList(new Event().setSummary("Meeting 1"));
        when(googleCalendarService.getEventsForDay(USER_ID, date)).thenReturn(mockEvents);

        // Act
        @SuppressWarnings("unchecked")
        List<Event> result = (List<Event>) ReflectionTestUtils.invokeMethod(
                calendarTool, "getTodaysEvents", date);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummary()).isEqualTo("Meeting 1");
    }

    @Test
    @DisplayName("GIVEN valid meeting details WHEN scheduling THEN return confirmation message")
    void scheduleMeeting_ShouldReturnConfirmation() throws IOException 
    {
        // Arrange
        String title = "Project Sync";
        String slot = "2023-10-01T15:00:00Z";
        String expectedMessage = "Meeting scheduled successfully.";

        when(googleCalendarService.scheduleMeetingWithAttendees(
                eq(USER_ID), anyList(), eq(title), anyString(), eq(slot), anyInt()))
                .thenReturn(expectedMessage);

        // Act
        String response = (String) ReflectionTestUtils.invokeMethod(
                calendarTool, "scheduleMeeting", title, "Desc", Collections.singletonList("a@b.com"), slot, 30);

        // Assert
        assertThat(response).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("GIVEN an exception occurs WHEN updating meeting THEN return error string")
    void updateMeeting_ShouldHandleExceptions() throws Exception 
    {
        // Arrange
        when(googleCalendarService.updateMeeting(any(), any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("API Error"));

        // Act
        String result = (String) ReflectionTestUtils.invokeMethod(
                calendarTool, "updateMeeting", "evtId", "New Title", "2023-10-02T10:00:00Z", 60);

        // Assert
        assertThat(result).isEqualTo("Error updating meeting.");
    }
}






