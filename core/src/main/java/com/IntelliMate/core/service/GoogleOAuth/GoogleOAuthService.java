package com.IntelliMate.core.service.GoogleOAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import org.springframework.beans.factory.annotation.Value;




@Service
public class GoogleOAuthService 
{
	@Value("${google.oauth.client.secrets.path}")
    private String clientSecretsPath;
	
	// Handles the main google auth flow
	private GoogleAuthorizationCodeFlow flow;
	
	// handles the http connections and requests
	private HttpTransport httpTransport;
	
	// handles the JSON responses from google
	private JsonFactory jsonFactory;
	
	// Defines the scopes for my app
	private List<String> scopes;
	
	// Directory to store user tokens
	private String TOKENS_DIRECTORY_PATH = "tokens";
	
	
	
	public GoogleOAuthService() throws IOException, GeneralSecurityException
	{
        this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        // Initialize jsonFactory
        this.jsonFactory = GsonFactory.getDefaultInstance();
        
        // Define scopes
        this.scopes = Arrays.asList(
            "https://www.googleapis.com/auth/contacts.readonly",
            "https://www.googleapis.com/auth/gmail.send",
            "https://www.googleapis.com/auth/gmail.compose",
            "https://www.googleapis.com/auth/calendar"
        );
        
        // Load client secrets from file
        GoogleClientSecrets clientSecrets = loadClientSecrets();
        
        // Build the flow
        this.flow = new GoogleAuthorizationCodeFlow.Builder(
            httpTransport,
            jsonFactory,
            clientSecrets,
            scopes
        )
        		
        .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
        .setAccessType("offline")
        .build();
    }
	
	// Helper method to load client_secret.json
    private GoogleClientSecrets loadClientSecrets() throws IOException 
    {
    	FileInputStream fis = new FileInputStream(clientSecretsPath);
        return GoogleClientSecrets.load(jsonFactory, new InputStreamReader(fis));
    }
    
    // Method to get the authorization url, afer this user gets option to "continue with google login"
    public String getAuthorizationUrl() throws IOException 
    {
        return flow.newAuthorizationUrl()
        	   .setRedirectUri("http://localhost:8080/oauth2/callback") // This is where google will redirect after auth
               .setAccessType("offline") // Request refresh token
               .setApprovalPrompt("force") // Force approval prompt every time
               .build();
    }
    
    // Method to exchange user authorization code for tokens
    public Credential exchangeCodeForTokens(String code) throws IOException 
    {
        TokenResponse tokenResponse = flow.newTokenRequest(code)
            .setRedirectUri("http://localhost:8080/oauth2/callback")
            .execute();
        
        return flow.createAndStoreCredential(tokenResponse, "user");
    }
    
    
    // Method to get stored credentials for a user
    public Credential getStoredCredential(String userId) throws IOException 
    {
        // Load and return credential from the data store
        // Token refresh is handled automatically when credential is used
        return flow.loadCredential(userId);
    }

    // Method to revoke user's access token
    public void revokeAccess(String userId) throws IOException 
    {
        // Load the stored credential
        Credential credential = flow.loadCredential(userId);
        
        if(credential != null && credential.getAccessToken() != null) 
        {
            try 
            {
                // Revoke the token with Google's revocation endpoint
                httpTransport.createRequestFactory()
                    	.buildGetRequest(new com.google.api.client.http.GenericUrl(
                        "https://oauth2.googleapis.com/revoke?token=" + credential.getAccessToken()))
                    	.execute();
            } 
            
            catch(IOException e) 
            {
            	System.err.println("Error revoking token with Google: " + e.getMessage());
            }
            
            // Remove the stored credential from the data store
            
            finally
            {
            	// Delete the stored credentials locally
	            flow.getCredentialDataStore().delete(userId);
            }
        }
    }
}







































