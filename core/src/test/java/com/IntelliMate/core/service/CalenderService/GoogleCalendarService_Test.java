package com.IntelliMate.core.service.CalenderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.service.CalendarService.GoogleCalendarService;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.api.services.calendar.model.FreeBusyResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.Collections;
import java.util.List;




@ExtendWith(MockitoExtension.class)
class GoogleCalendarService_Test 
{
    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private Credential credential;

    @Mock
    private Calendar googleCalendarClient;

    @Mock
    private Calendar.Events eventsInternal;

    @Mock
    private Calendar.Events.List eventsListRequest;

    @Spy
    @InjectMocks
    private GoogleCalendarService googleCalendarService;

    private final String USER_ID = "test-user-001";

    
    
    
    @BeforeEach
    void setUp() throws IOException 
    {
        lenient().doReturn(googleCalendarClient)
                .when(googleCalendarService).getGoogleCalenderService(USER_ID);
    }

    @Test
    @DisplayName("Should fetch events for a specific day")
    void test_GetEventsForDay() throws IOException 
    {
        // Arrange
        String date = "2026-01-11";
        Events mockEvents = new Events().setItems(Collections.singletonList(new Event().setSummary("Test Meeting")));

        when(googleCalendarClient.events()).thenReturn(eventsInternal);
        when(eventsInternal.list("primary")).thenReturn(eventsListRequest);
        when(eventsListRequest.setTimeMin(any())).thenReturn(eventsListRequest);
        when(eventsListRequest.setTimeMax(any())).thenReturn(eventsListRequest);
        when(eventsListRequest.setOrderBy(anyString())).thenReturn(eventsListRequest);
        when(eventsListRequest.setSingleEvents(anyBoolean())).thenReturn(eventsListRequest);
        when(eventsListRequest.execute()).thenReturn(mockEvents);

        // Act
        List<Event> result = googleCalendarService.getEventsForDay(USER_ID, date);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSummary()).isEqualTo("Test Meeting");
        verify(eventsListRequest, times(1)).execute();
    }

    @Test
    @DisplayName("Should throw exception when user is not authenticated")
    void test_GetEvents_Unauthenticated() throws IOException 
    {
        // Overwrite the spy for this specific failure case
        doThrow(new IllegalStateException("User not authenticated"))
                .when(googleCalendarService).getGoogleCalenderService("unauth-user");

        assertThrows(IllegalStateException.class, () -> 
        {
            googleCalendarService.getEventsForDay("unauth-user", "2026-01-11");
        });
    }

    @Test
    @DisplayName("Should return available slots when busy list is empty")
    void test_GetNextAvailableSlots_Success() throws IOException 
    {
        // Arrange
        List<String> attendees = List.of("attendee@test.com");
        int duration = 30;

        // Mocking the FreeBusy response
        FreeBusyResponse mockResponse = new FreeBusyResponse();
        mockResponse.setCalendars(Collections.emptyMap()); // Empty map implies free

        // mock the checkFreeBusy call which is used inside the loop
        doReturn(mockResponse).when(googleCalendarService)
                .checkFreeBusy(anyString(), anyList(), anyString(), anyString());

        // Act
        List<com.google.api.client.util.DateTime> slots = 
                googleCalendarService.getNextAvailableSlots(USER_ID, attendees, duration);

        // Assert
        assertThat(slots).isNotEmpty();
    }

    @Test
    @DisplayName("Should update meeting title and return success message")
    void test_UpdateMeeting_Success() throws IOException 
    {
        // Arrange
        String eventId = "evt_123";
        Event existingEvent = new Event().setSummary("Old Title");
        
        Calendar.Events.Get getRequest = mock(Calendar.Events.Get.class);
        Calendar.Events.Update updateRequest = mock(Calendar.Events.Update.class);

        when(googleCalendarClient.events()).thenReturn(eventsInternal);
        when(eventsInternal.get("primary", eventId)).thenReturn(getRequest);
        when(getRequest.execute()).thenReturn(existingEvent);
        
        when(eventsInternal.update(eq("primary"), eq(eventId), any(Event.class))).thenReturn(updateRequest);
        when(updateRequest.setSendUpdates(anyString())).thenReturn(updateRequest);
        when(updateRequest.setConferenceDataVersion(anyInt())).thenReturn(updateRequest);
        when(updateRequest.execute()).thenReturn(new Event().setSummary("New Title"));

        // Act
        String status = googleCalendarService.updateMeeting(USER_ID, eventId, "New Title", null, null);

        // Assert
        assertThat(status).contains("Success", "New Title");
    }

    @Test
    @DisplayName("Should handle deletion of an event")
    void test_DeleteCalendarEvent() throws IOException 
    {
        // Arrange
        String eventId = "evt_delete_99";
        Calendar.Events.Delete deleteRequest = mock(Calendar.Events.Delete.class);

        when(googleCalendarClient.events()).thenReturn(eventsInternal);
        when(eventsInternal.delete("primary", eventId)).thenReturn(deleteRequest);
        when(deleteRequest.setSendUpdates("all")).thenReturn(deleteRequest);

        // Act
        String result = googleCalendarService.deleteCalendarEvent(USER_ID, eventId);

        // Assert
        assertThat(result).contains("Success", eventId);
        verify(deleteRequest).execute();
    }
}




