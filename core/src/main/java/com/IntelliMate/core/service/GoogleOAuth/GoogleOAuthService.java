package com.IntelliMate.core.service.GoogleOAuth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import jakarta.annotation.PostConstruct;
import com.IntelliMate.core.repository.GoogleUserTokenRepo;
import com.IntelliMate.core.repository.GoogleAuthUserToken;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import java.time.ZoneId;





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
	
	// Repository to manage user tokens
	private GoogleUserTokenRepo GoogleUserTokenRepo;
	
	// Client secrets loaded from file
	private GoogleClientSecrets clientSecrets;
	
	
	
	
	
	public GoogleOAuthService() {}
	
	// Initialize the service
	@PostConstruct
	public void init() throws IOException, GeneralSecurityException
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
        this.clientSecrets = loadClientSecrets();
        
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
    public void exchangeCodeForTokens(String Authcode) throws IOException 
    {
    	GoogleAuthUserToken userToken = new GoogleAuthUserToken();
    	
    	// Exchange authorization code for tokens
        TokenResponse tokenResponse = flow.newTokenRequest(Authcode)
            .setRedirectUri("http://localhost:8080/oauth2/callback")
            .execute();
        
        
        String refreshToken = tokenResponse.getRefreshToken();
        
        // Check if user token already exists
        if(GoogleUserTokenRepo.existsByRefreshToken(refreshToken))
        {
        	userToken = GoogleUserTokenRepo.findByRefreshToken(refreshToken);
        	
        	// Update existing token
        	userToken.setAccessToken(tokenResponse.getAccessToken());
        	userToken.setTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
        	userToken.setUpdatedAt(LocalDateTime.now());
        	
        	GoogleUserTokenRepo.save(userToken);
		}
        
        else
        {
        	// Store new token in the database
            userToken.setAccessToken(tokenResponse.getAccessToken());
            userToken.setRefreshToken(tokenResponse.getRefreshToken());
            userToken.setTokenExpiry(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresInSeconds()));
            userToken.setCreatedAt(LocalDateTime.now());
    		userToken.setUpdatedAt(LocalDateTime.now());
            
            GoogleUserTokenRepo.save(userToken);        	
        }       
    }
    
    
    // Method to get stored credential for a user
    public Credential getStoredCredential(Long userID) throws IOException 
    {
        GoogleAuthUserToken userToken = GoogleUserTokenRepo.findByUserId(userID);
        		
        if(userToken == null) throw new IOException("No token found for user ID: " + userID);
        
        // Check if token expired
        if(userToken.getTokenExpiry().isBefore(LocalDateTime.now())) 
        {
        	// If expired, refresh the token
            refreshAccessToken(userID);
            
            // Reload the updated token
            userToken = GoogleUserTokenRepo.findByUserId(userID);
            
            if(userToken == null) throw new IOException("No token found for user ID after refresh: " + userID);
        }
        
        // Build Credential using the information from userToken
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
            .setClientAuthentication(new ClientParametersAuthentication(
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret()))
            .build();
        
        credential.setAccessToken(userToken.getAccessToken());
        credential.setRefreshToken(userToken.getRefreshToken());
        credential.setExpirationTimeMilliseconds(
        userToken.getTokenExpiry().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
        
        return credential;
    }
    
    // Refresh expired token
    private void refreshAccessToken(Long userID) throws IOException 
    {
        
    	GoogleAuthUserToken userToken = GoogleUserTokenRepo.findByUserId(userID);
        		
    	if(userToken == null) throw new IOException("No token found for user ID: " + userID);
        
        if(userToken.getRefreshToken() == null) throw new IOException("No refresh token. User needs to re-authenticate.");
        
        // Build Credential using the information from userToken
        Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(httpTransport)
            .setJsonFactory(jsonFactory)
            .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
            .setClientAuthentication(new ClientParametersAuthentication(
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret()))
            .build();
        
        // Fetch the new access token using the refresh token
        credential.setRefreshToken(userToken.getRefreshToken());
        credential.refreshToken();
        
        // Update the stored token in the database using the new token info
        userToken.setAccessToken(credential.getAccessToken());
        userToken.setTokenExpiry(LocalDateTime.now().plusSeconds(credential.getExpiresInSeconds()));
        userToken.setUpdatedAt(LocalDateTime.now());
        
        // Save updated token
        GoogleUserTokenRepo.save(userToken);
    }

    // Method to revoke user's access token
    public void revokeAccess(Long userID) throws IOException 
    {
        // Load the stored credential from DB
    	GoogleAuthUserToken userToken = GoogleUserTokenRepo.existsByUserId(userID) ? GoogleUserTokenRepo.findByUserId(userID) : null;
        
    	
        if(userToken != null && userToken.getAccessToken() != null) 
        {
            try 
            {
                // Revoke the token with Google's revocation endpoint
                httpTransport.createRequestFactory()
                    	.buildGetRequest(new com.google.api.client.http.GenericUrl(
                        "https://oauth2.googleapis.com/revoke?token=" + userToken.getAccessToken()))
                    	.execute();
            } 
            
            catch(IOException e) 
            {
            	System.err.println("Error revoking token with Google: " + e.getMessage());
            }
            
            // Remove the stored credential from the database
            finally
            {
            	// Delete the stored credentials locally
            	GoogleUserTokenRepo.deleteByUserId(userID);
            }
        }
    }
}







































