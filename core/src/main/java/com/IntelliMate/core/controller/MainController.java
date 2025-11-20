package com.IntelliMate.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import com.IntelliMate.core.service.JWTService.JWTTokenService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;
import com.IntelliMate.core.AIEngine;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.IntelliMate.core.service.EncryptionService.JasyptEncryptionService;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;




@Controller
@RequestMapping("/api")
public class MainController 
{
	private final AIEngine aiEngine;
	private final GoogleOAuthService googleOAuthService;
	private final JWTTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final JasyptEncryptionService jasyptEncryptionService;
	
	
	public MainController(AIEngine aiEngine, GoogleOAuthService googleOAuthService, 
			              JWTTokenService jwtTokenService, UserRepository userRepository,
			              JasyptEncryptionService jasyptEncryptionService) 
	{
		this.aiEngine = aiEngine;
		this.googleOAuthService = googleOAuthService;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.jasyptEncryptionService = jasyptEncryptionService;
	}
	
	// Serve login page
	@GetMapping("/")
	public String getLoginPage() 
	{
		return "login"; 
	}
	
	// Serve dashboard page
	@GetMapping("/dashboard")
	public String getDashboardPage(@RequestParam("token") String jwtToken,
			                       HttpServletResponse response)
	{
		// Create JWT token in secure HttpOnly cookie
		// and send it to client to save in browser
		Cookie cookie = new Cookie("jwt", jwtToken);
		cookie.setHttpOnly(true);                  // JS cannot read
		cookie.setSecure(true);                    // HTTPS only
		cookie.setPath("/api/chat");               // valid for chat endpoint only
		cookie.setMaxAge(7 * 24 * 60 * 60);        // 7 days
		response.addCookie(cookie);

		return "Dashboard"; 
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
            String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
            
            return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
        } 
        
        catch(IOException e) 
        {
            // Authentication failed, redirect to login with error
            String errorUrl = "http://localhost:8080/login?error=authentication_failed";
            
            return ResponseEntity.status(302)
                .header("Location", errorUrl)
                .build();
        }
    }
    
    
    // Handle user registration
    @PostMapping("/UserRegistration")
    public ResponseEntity<Object> registerUser(@RequestParam("email") String email, @RequestParam("password") String password, 
    		                   HttpSession session) 
	{
    	String userName = email;
		String userPassword = password;
		String encryptedUserPassword = jasyptEncryptionService.encrypt(userPassword);
		
		User newUser = new User(userName, encryptedUserPassword, java.time.Instant.now().toString());
		
		// Save user to database
		userRepository.save(newUser);
		
		// Create JWT token for the user
		String jwtToken = jwtTokenService.generateToken(userName);
		
		// Redirect to dashboard with JWT token
		String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
        
        return ResponseEntity.status(302)
            .header("Location", redirectUrl)
            .build();
	}
    
    
    @PostMapping("/UserLogin")
    public ResponseEntity<Object> userLogin(@RequestParam String email, @RequestParam String password)
    {
    	String inputEmail = email;
    	String inputPassword = password;
    	
    	// Retrieve user from database
    	User user = userRepository.findByEmail(inputEmail);
    	
    	if(user == null)
    	{
    		return ResponseEntity
			        .status(HttpStatus.FOUND)     // 302
			        .header(HttpHeaders.LOCATION, "http://localhost:8080/?error=user_not_found")
			        .build();
    	}
    	
    	// Get stored encrypted password
    	String userStoredPassword = user.getPassword();
 
    	if(inputPassword != null && inputPassword.equals(jasyptEncryptionService.decrypt(userStoredPassword)))
		{
    		// Create JWT token for the user
    		String jwtToken = jwtTokenService.generateToken(email);
    			
    		// Redirect to dashboard with JWT token
    		String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
    	        
    	    return ResponseEntity.status(302)
    	        .header("Location", redirectUrl)
    	        .build();
		}
    		
    	else
		{
    		// Password mismatch → redirect to login page with error message
    		return ResponseEntity
    			    .status(HttpStatus.FOUND)     // 302
    			    .header(HttpHeaders.LOCATION, "http://localhost:8080/?error=invalid_credentials")
    			    .build();
		}
    }
    
	@PostMapping("/chat")
	public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> userInput, @CookieValue(name = "jwtToken", required = false) String token)			                                        
	{
		// If cookie missing or expired token → unauthorized
	    if (token == null || !jwtTokenService.isValid(token)) 
	    {
	        return ResponseEntity.status(401).body(
	                Map.of("error", "Missing or invalid JWT cookie")
	        );
	    }
		
		// Get user ID from token to pass to LLM which will pass it to tools
		String userID = jwtTokenService.extractUserInfo(token);
		
		// Get user message
		String userMessage = userInput.get("message");

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



