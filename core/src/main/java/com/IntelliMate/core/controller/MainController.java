package com.IntelliMate.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;
import com.IntelliMate.core.AIEngine;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;



@RestController
@RequestMapping("/api")
public class MainController 
{
	private final AIEngine aiEngine;
	private final GoogleOAuthService googleOAuthService;
	
	
	
	public MainController(AIEngine aiEngine, GoogleOAuthService googleOAuthService) 
	{
		this.aiEngine = aiEngine;
		this.googleOAuthService = googleOAuthService;
	}
	
	
	@GetMapping("/google/login")
    public ResponseEntity<String> initiateGoogleLogin() 
	{
        try 
        {
            // Generate Google's authorization URL
            String authUrl = googleOAuthService.getAuthorizationUrl();
            
            // Return URL to frontend
            return ResponseEntity.ok(authUrl);
            
        } 
        
        catch(IOException e) 
        {
            return ResponseEntity.status(500).body("Failed to generate Google login URL");
        }
    }
	

    // Step 2: Google redirects user back here after authentication
    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) 
    {
        try 
        {
            // Exchange authorization code for tokens and get user email
            String userEmail = googleOAuthService.exchangeCodeForTokens(code, );
            
            // Tokens are now saved in database
            // Redirect to frontend dashboard with user info
            String redirectUrl = "http://localhost:3000/dashboard?email=" + userEmail + "&status=success";
            
            return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
            
        } 
        
        catch(IOException e) 
        {
            // Authentication failed, redirect to login with error
            String errorUrl = "http://localhost:3000/login?error=authentication_failed";
            
            return ResponseEntity.status(302)
                .header("Location", errorUrl)
                .build();
        }
    }
	
	
	@PostMapping("/chat")
	public ResponseEntity<Map<String, String>> chat (@RequestBody String query) 
	{
		String userMessage = query;

		if(userMessage == null || userMessage.trim().isEmpty()) 
		{
			return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
		}
		
		try
		{
			String AIResponse = aiEngine.chat(userMessage);
			
			
			
			System.out.println("AIResponse from Main controller: " + AIResponse);
			
			
			
			
			return ResponseEntity.ok(Map.of("response", AIResponse));
		}
		
		catch(Exception e)
		{
			return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
		}
	}
}



