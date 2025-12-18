package com.IntelliMate.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import com.IntelliMate.core.service.JWTService.JWTTokenService;
import java.util.UUID;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import com.IntelliMate.core.AIEngine;
import com.IntelliMate.core.repository.ConversationHistory;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.IntelliMate.core.service.EncryptionService.JasyptEncryptionService;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import com.IntelliMate.core.repository.ConversationHistoryRepository;




@Controller
@RequestMapping("/api")
public class MainController 
{
	private final AIEngine aiEngine;
	private final GoogleOAuthService googleOAuthService;
	private final JWTTokenService jwtTokenService;
	private final UserRepository userRepository;
	private final JasyptEncryptionService jasyptEncryptionService;
	private final ConversationHistoryRepository ConversationHistoryRepository;
	
	
	public MainController(AIEngine aiEngine, GoogleOAuthService googleOAuthService, 
			              JWTTokenService jwtTokenService, UserRepository userRepository,
			              JasyptEncryptionService jasyptEncryptionService,
			              ConversationHistoryRepository ConversationHistoryRepository) 
	{
		this.aiEngine = aiEngine;
		this.googleOAuthService = googleOAuthService;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.jasyptEncryptionService = jasyptEncryptionService;
		this.ConversationHistoryRepository = ConversationHistoryRepository;
	}
	
	// Serve login page
	@GetMapping("/")
	public String getLoginPage() 
	{
		return "login"; 
	}
	
	// Serve registration page
	@GetMapping("/registration")
	public String getRegistrationPage() 
	{
		return "registration"; 
	}
	
	// Serve dashboard page
	@GetMapping("/dashboard")
	public String getDashboardPage(@RequestParam("token") String jwtToken,
			                       HttpServletResponse response)
	{
		// Create JWT token in secure HttpOnly cookie
		// and send it to client to save in browser
		Cookie cookie = new Cookie("jwt", jwtToken);
		cookie.setHttpOnly(true);                  	// JS cannot read
		cookie.setSecure(false);                    // HTTPS only
		cookie.setPath("/");               			// valid for all endpoints
		cookie.setMaxAge(7 * 24 * 60 * 60);        	// 7 days
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
	        // Get Google tokens and user info
	        User googleUser = googleOAuthService.exchangeCodeForTokens(code);
	        
	        // Check if user exists in YOUR database by email
	        User existingUser = userRepository.findByEmail(googleUser.getEmail());
	        
	        
	        if(existingUser == null) 
	        {
	            // NEW USER - Create account
	            User newUser = new User();
	            newUser.setId(UUID.randomUUID().toString());
	            newUser.setEmail(googleUser.getEmail());
	            newUser.setGoogleId(googleUser.getGoogleId());
	            newUser.setGoogleAccessToken(googleUser.getGoogleAccessToken());
	            newUser.setGoogleRefreshToken(googleUser.getGoogleRefreshToken());
	            newUser.setAuthMethod("google");
	            
	            userRepository.save(newUser);
	            
	            // Generate JWT for YOUR app
	            String jwtToken = jwtTokenService.generateToken(newUser.getEmail());
	            
	            String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
	            
	            // Redirect to dashboard with JWT token
	            return ResponseEntity.status(302)
	            	   .header("Location", redirectUrl)
	                   .build();
	        }
	        
	        else 
	        {
	            // EXISTING USER - Link Google account
	            existingUser.setGoogleId(googleUser.getGoogleId());
	            existingUser.setGoogleAccessToken(googleUser.getGoogleAccessToken());
	            existingUser.setGoogleRefreshToken(googleUser.getGoogleRefreshToken());
	            existingUser.setAuthMethod("both");  // Now supports both methods
	            existingUser.setLastLogin(LocalDateTime.now());
	            
	            userRepository.save(existingUser);
	            
	            // Generate JWT using existing user ID
	            String jwtToken = jwtTokenService.generateToken(existingUser.getEmail());
	            
	            String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
	            
	            // Redirect to dashboard with JWT token
	            return ResponseEntity.status(302)
	                   .header("Location", redirectUrl)
	                   .build();
	        }
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
		
		User newUser = new User(userName, encryptedUserPassword, LocalDateTime.now());
		
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
    
    // Handle user login
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
    
    // Handle chat messages
	@PostMapping("/chat")
	public ResponseEntity<Map<String, String>> chat(HttpSession session,
													@RequestBody Map<String, String> userInput, 	
			                                        @CookieValue(name = "jwt", required = false) String token)			                                        
	{
		// If cookie missing or expired token → unauthorized
	    if(token == null || !jwtTokenService.isValid(token)) 
	    {
	        return ResponseEntity.status(403).body(
	                Map.of("error", "Missing or invalid JWT cookie")
	        );
	    }
		
		// Get user ID from token to pass to LLM which will pass it to tools
	    String userID = jwtTokenService.extractUserInfo(token);
	    
	    // Fetch the User entity object to link with conversation history
	    User currentUser = userRepository.findByEmail(userID);
	    
	    if(currentUser == null) 
	    {
	        return ResponseEntity.status(404).body(Map.of("error", "User not found"));
	    }
		
		// Get user message
		String userMessage = userInput.get("message");
		
		// Loading last 10 messages from conversation history into the LLM memory
		aiEngine.getMemory().clear(); // clear existing memory
		
		// fetch last 10 messages from DB
		List<ConversationHistory> history = ConversationHistoryRepository.getLastNConversationByUser_id(10, userID);
		
		if(!history.isEmpty())
		{
			// get memory instance
			MessageWindowChatMemory memory = aiEngine.getMemory();
			
			// load messages into memory
			for(ConversationHistory userHistory : history)
			{
				memory.add(new UserMessage(userHistory.getMessage()));
				memory.add(new AiMessage(userHistory.getResponse()));
			}
			
			// mark memory as loaded in session to avoid reloading every time user sends a message
			session.setAttribute("memoryLoaded", true);
		}
		
		// Validate message
		if(userMessage == null || userMessage.trim().isEmpty()) 
		{
			return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
		}
		
		try
		{
			// Send message and get AI response
			String AIResponse = aiEngine.chat(userMessage, Map.of("userID", userID));
			
			// save conversation to DB
			ConversationHistory userConversationHistory = new ConversationHistory(userMessage, 
																			      AIResponse, 
																			      LocalDateTime.now());
			// link conversation to user
			userConversationHistory.setUser(currentUser);
			
			ConversationHistoryRepository.save(userConversationHistory);
			
			return ResponseEntity.ok(Map.of("response", AIResponse));
		}
		
		catch(Exception e)
		{
			// Handle any unexpected errors
			e.printStackTrace();
			return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
		}
	}
	
	// Handle user logout
	@GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) 
	{
        // delete JWT cookie
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);

        // invalidate session
        session.invalidate();

        return "redirect:/login";
    }
}



