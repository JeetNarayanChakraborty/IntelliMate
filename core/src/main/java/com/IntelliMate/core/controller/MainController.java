package com.IntelliMate.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import com.IntelliMate.core.service.JWTService.JWTTokenService;
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
	private final JWTTokenService jwtTokenService;
	
	
	public MainController(AIEngine aiEngine, GoogleOAuthService googleOAuthService, JWTTokenService jwtTokenService) 
	{
		this.aiEngine = aiEngine;
		this.googleOAuthService = googleOAuthService;
		this.jwtTokenService = jwtTokenService;
	}
	
	// Serve login page
	@GetMapping("/")
	public String showLoginPage() 
	{
		return "login"; 
	}
	
	// Get the Google OAuth login URL
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
	

    // Google redirects user back here after authentication
    @GetMapping("/oauth2/callback")
    public ResponseEntity<?> handleGoogleCallback(@RequestParam String code) 
    {
        try 
        {
            // Exchange authorization code for tokens and get user ID
            String userID = googleOAuthService.exchangeCodeForTokens(code);
            
            // Generate JWT token
            String jwtToken = jwtTokenService.generateToken(userID);
            
            // Redirect to dashboard with JWT token
            String redirectUrl = "http://localhost:3000/dashboard?token=" + jwtToken + "&status=success";
            
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
	public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> request, 
			                                        @RequestHeader(value = "Authorization", required = false) String authHeader) 
	{
		// Validate JWT token
		if(authHeader == null || !authHeader.startsWith("Bearer "))
		{
			return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
		}
		
		// Extract token
		String token = authHeader.replace("Bearer ", "");
		
		// Validate if token is valid
		if(!jwtTokenService.isValid(token))
		{
			return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
		}
		
		// Get user ID from token to pass to LLM which will pass it to tools
		String userID = jwtTokenService.extractUserID(token);
		
		// Get user message
		String userMessage = request.get("message");

		// Validate message
		if(userMessage == null || userMessage.trim().isEmpty()) 
		{
			return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
		}
		
		try
		{
			// Send message and get AI response
			String AIResponse = aiEngine.chat(userMessage, Map.of("userID", userID));
			return ResponseEntity.ok(Map.of("response", AIResponse));
		}
		
		catch(Exception e)
		{
			// Handle any unexpected errors
			return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
		}
	}
}



