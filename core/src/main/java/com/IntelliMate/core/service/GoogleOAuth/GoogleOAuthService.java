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
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;

import jakarta.annotation.PostConstruct;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
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
	
	// Repository to manage user and user google tokens
	private UserRepository userRepository;
	
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
        		"https://www.googleapis.com/auth/userinfo.email",
        		"https://www.googleapis.com/auth/userinfo.profile",
        		"https://www.googleapis.com/auth/gmail.readonly",
        		"https://www.googleapis.com/auth/gmail.send",
        		"https://www.googleapis.com/auth/gmail.compose",
        		"https://www.googleapis.com/auth/gmail.modify",
        		"https://www.googleapis.com/auth/calendar",
        		"https://www.googleapis.com/auth/contacts.readonly"
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
    
    // Method to get the authorization URL, after this user gets option to "continue with google login"
    public String getAuthorizationUrl() throws IOException 
    {
        String url = flow.newAuthorizationUrl()
        	   .setRedirectUri("http://localhost:8080/api/oauth2/callback") // This is where google will redirect after auth
               .setAccessType("offline") // Request refresh token
               .setApprovalPrompt("force") // Force approval prompt every time
               .build();
        
        
        
        // URL Log for debugging
        System.out.println("Generated Auth URL: " + url);
        
        
        
        
        return url;
    }
    
    // Method to exchange user authorization code for tokens
    public User exchangeCodeForTokens(String Authcode) throws IOException 
    {
    	// Exchange authorization code for token
        TokenResponse tokenResponse = flow.newTokenRequest(Authcode)
            .setRedirectUri("http://localhost:8080/api/oauth2/callback")
            .execute();
        
        // Create credential with the token
    	Credential credential = new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
    	    .setTransport(new NetHttpTransport())
    	    .setJsonFactory(GsonFactory.getDefaultInstance())
    	    .build()
    	    .setAccessToken(tokenResponse.getAccessToken());
    	
    	// Get user info from Google
        Oauth2 oauth2 = new Oauth2.Builder(new NetHttpTransport(), 
                                           GsonFactory.getDefaultInstance(), 
                                           credential)
            .setApplicationName("IntelliMate")
            .build();
        
        Userinfo userInfo = oauth2.userinfo().get().execute();
        
        // Extract user details
        String email = userInfo.getEmail();
        String googleId = userInfo.getId();
    	String accessToken = tokenResponse.getAccessToken();
    	String refreshToken = tokenResponse.getRefreshToken();
    	
    	
    	return new User(email, googleId, accessToken, refreshToken, null);    	
    }
    
    // Method to get stored credential for a user
    public Credential getStoredCredential(String userID) throws IOException 
    {
        User userToken = userRepository.findByEmail(userID);
        		
        if(userToken == null) throw new IOException("No user token found for user ID: " + userID);
        
        // Check if token expired
        if(userToken.getGoogleTokenExpiry().isBefore(LocalDateTime.now())) 
        {
        	// If expired, refresh the token
        	refreshAccessToken(userID);
            
            // Reload the updated token
            userToken = userRepository.findByEmail(userID);
            
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
        
        credential.setAccessToken(userToken.getGoogleAccessToken());
        credential.setRefreshToken(userToken.getGoogleRefreshToken());
        credential.setExpirationTimeMilliseconds(
        userToken.getGoogleTokenExpiry().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        );
        
        return credential;
    }
    
    // Refresh expired token
    private void refreshAccessToken(String userID) throws IOException 
    {    
    	User userToken = userRepository.findByEmail(userID);
        		
    	if(userToken == null) throw new IOException("No token found for user ID: " + userID);
        
        if(userToken.getGoogleRefreshToken() == null) throw new IOException("No refresh token. User needs to re-authenticate.");
        
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
        credential.setRefreshToken(userToken.getGoogleRefreshToken());
        credential.refreshToken();
        
        // Update the stored token in the database using the new token info
        userToken.setGoogleAccessToken(credential.getAccessToken());
        userToken.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(credential.getExpiresInSeconds()));
        userToken.setUpdatedAt(LocalDateTime.now());
        
        // Save updated token
        userRepository.save(userToken);
    }

    // Method to revoke user's access token
    public void revokeAccess(String userID) throws IOException 
    {
        // Load the stored credential from DB
    	User userToken = userRepository.existsByEmail(userID) ? userRepository.findByEmail(userID) : null;
        
    	
        if(userToken != null && userToken.getGoogleAccessToken() != null) 
        {
            try 
            {
                // Revoke the token with Google's revocation endpoint
                httpTransport.createRequestFactory()
                    	.buildGetRequest(new com.google.api.client.http.GenericUrl(
                        "https://oauth2.googleapis.com/revoke?token=" + userToken.getGoogleAccessToken()))
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
            	userRepository.deleteByEmail(userID);
            }
        }
    }
}







































