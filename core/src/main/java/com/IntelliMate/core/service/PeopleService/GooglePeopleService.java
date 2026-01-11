package com.IntelliMate.core.service.PeopleService;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.SearchResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import com.IntelliMate.core.repository.ContactInfo;
import com.google.api.services.people.v1.model.SearchResult;




@Service
public class GooglePeopleService 
{
	private final GoogleOAuthService googleOAuthService;

	
	
    public GooglePeopleService(GoogleOAuthService googleOAuthService) 
    {
        this.googleOAuthService = googleOAuthService;
    }
	
    // Get all contacts
    public List<ContactInfo> getAllContacts(String userId) throws IOException 
    {
    	// Initialize People service
        PeopleService service = getPeopleService(userId);
        
        // Fetch connections from People API
        ListConnectionsResponse response = service.people().connections()
            .list("people/me")
            .setPageSize(1000)
            .setPersonFields("names,emailAddresses")
            .execute();
        
        List<Person> connections = response.getConnections();
        List<ContactInfo> contacts = new ArrayList<>();
        
        // Helper: Extract contact info from Person object
        if(connections != null && !connections.isEmpty()) 
        {
            for(Person person : connections) 
            {
            	// Extract contact info
                ContactInfo contact = extractContactInfo(person);
                
                if(contact != null) 
                {
                    contacts.add(contact);
                }
            }
        }
        
        return contacts;
    }
    
    // Get email for a contact name
    public String getEmailByName(String userId, String name) throws IOException 
    {
    	PeopleService service = getPeopleService(userId);
        
        // Direct search API
        SearchResponse response = service.people().searchContacts()
            .setQuery(name)
            .setReadMask("names,emailAddresses")
            .execute();
        
        List<SearchResult> results = response.getResults();
        
        if(results == null || results.isEmpty()) 
        {
            return null; // No contact found
        }
        
        // Get first match
        Person person = results.get(0).getPerson();
    	
        ContactInfo contact = extractContactInfo(person);
        return contact != null ? contact.getEmail() : null;
    }
    
    // Helper: Extract contact info from Person object
    private ContactInfo extractContactInfo(Person person) 
    {
        String name = null;
        String email = null;
        
        // Get the name of the person
        if(person.getNames() != null && !person.getNames().isEmpty()) 
        {
            Name personName = person.getNames().get(0);
            name = personName.getDisplayName();
        }
        
        // Get the email of the person
        if(person.getEmailAddresses() != null && !person.getEmailAddresses().isEmpty()) 
        {
            EmailAddress emailAddress = person.getEmailAddresses().get(0);
            email = emailAddress.getValue();
        }
        
        // Only return if both name and email exist
        if(name != null && email != null) 
        {
            return new ContactInfo(name, email);
        }
        
        return null;
    }
    
    
    // Helper: Get People service
    public PeopleService getPeopleService(String userId) throws IOException 
    {
        Credential credential = googleOAuthService.getStoredCredential(userId);
        
        if (credential == null) 
        {
            throw new IllegalStateException("User not authenticated");
        }
        
        try 
        {
            return new PeopleService.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName("IntelliMate").build();
        } 
        catch (GeneralSecurityException e) 
        {
            throw new IOException("Failed to create People service", e);
        }
    }

    
    
    
    
}







