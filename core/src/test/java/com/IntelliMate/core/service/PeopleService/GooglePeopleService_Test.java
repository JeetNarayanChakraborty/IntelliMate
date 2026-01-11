package com.IntelliMate.core.service.PeopleService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.IntelliMate.core.repository.ContactInfo;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.*;
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
class GooglePeopleService_Test 
{
    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private PeopleService peopleService;

    @Mock
    private PeopleService.People peopleResource;

    @Spy
    @InjectMocks
    private GooglePeopleService googlePeopleService;

    private final String USER_ID = "tester@intellimate.com";

    
    
    @BeforeEach
    void setUp() throws IOException 
    {
        lenient().doReturn(peopleService).when(googlePeopleService).getPeopleService(USER_ID);
        lenient().when(peopleService.people()).thenReturn(peopleResource);
    }

    @Test
    @DisplayName("GIVEN a search name WHEN getEmailByName is called THEN return matching email")
    void getEmailByName_ShouldReturnFirstMatch() throws IOException 
    {
        // Arrange
        String searchName = "Alice";
        Person alice = new Person()
                .setNames(Collections.singletonList(new Name().setDisplayName("Alice Smith")))
                .setEmailAddresses(Collections.singletonList(new EmailAddress().setValue("alice@example.com")));

        SearchResponse searchResponse = new SearchResponse()
                .setResults(Collections.singletonList(new SearchResult().setPerson(alice)));

        PeopleService.People.SearchContacts searchMock = mock(PeopleService.People.SearchContacts.class);

        when(peopleResource.searchContacts()).thenReturn(searchMock);
        when(searchMock.setQuery(searchName)).thenReturn(searchMock);
        when(searchMock.setReadMask(anyString())).thenReturn(searchMock);
        when(searchMock.execute()).thenReturn(searchResponse);

        // Act
        String email = googlePeopleService.getEmailByName(USER_ID, searchName);

        // Assert
        assertThat(email).isEqualTo("alice@example.com");
        verify(searchMock).execute();
    }

    @Test
    @DisplayName("GIVEN user has connections WHEN getAllContacts is called THEN map to ContactInfo list")
    void getAllContacts_ShouldReturnMappedContacts() throws IOException 
    {
        // Arrange
        Person mockPerson = new Person()
                .setNames(Collections.singletonList(new Name().setDisplayName("John Doe")))
                .setEmailAddresses(Collections.singletonList(new EmailAddress().setValue("john@test.com")));

        ListConnectionsResponse response = new ListConnectionsResponse()
                .setConnections(Collections.singletonList(mockPerson));

        // Setup the connections hierarchy: peopleResource.connections().list()
        PeopleService.People.Connections connectionsMock = mock(PeopleService.People.Connections.class);
        PeopleService.People.Connections.List listRequest = mock(PeopleService.People.Connections.List.class);

        when(peopleResource.connections()).thenReturn(connectionsMock);
        when(connectionsMock.list("people/me")).thenReturn(listRequest);
        when(listRequest.setPageSize(anyInt())).thenReturn(listRequest);
        when(listRequest.setPersonFields(anyString())).thenReturn(listRequest);
        when(listRequest.execute()).thenReturn(response);

        // Act
        List<ContactInfo> result = googlePeopleService.getAllContacts(USER_ID);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John Doe");
        verify(listRequest).setPageSize(1000); // Verifying production parameters
    }
}


