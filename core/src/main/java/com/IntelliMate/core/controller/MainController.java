package com.IntelliMate.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import com.IntelliMate.core.service.JWTService.JWTTokenService;
import com.IntelliMate.core.service.UserService.UserService;
import com.IntelliMate.core.service.MailService.MailSendAndGetService;
import com.IntelliMate.core.service.SystemMailService.SystemMailService;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.IntelliMate.core.AIEngine;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import com.IntelliMate.core.service.EncryptionService.JasyptEncryptionService;
import com.IntelliMate.core.service.GoogleOAuth.GoogleOAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
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
	private final RememberMeServices rememberMeService;
	private final SystemMailService systemMailService;
	private final UserService userService;
	
	
	
	public MainController(AIEngine aiEngine, GoogleOAuthService googleOAuthService, 
			              JWTTokenService jwtTokenService, UserRepository userRepository,
			              JasyptEncryptionService jasyptEncryptionService,
			              RememberMeServices rememberMeService,
			              SystemMailService systemMailService,
			              MailSendAndGetService mailSendAndGetService,
			              UserService userService) 
	{
		this.aiEngine = aiEngine;
		this.googleOAuthService = googleOAuthService;
		this.jwtTokenService = jwtTokenService;
		this.userRepository = userRepository;
		this.jasyptEncryptionService = jasyptEncryptionService;
		this.rememberMeService = rememberMeService;
	    this.userService = userService;
	    this.systemMailService = systemMailService;
	}
	
	
	
	// Serve home page
	@GetMapping("/home")
	public String getHomePage() 
	{
		return "HomePage"; 
	}
	
	// Serve login page
	@GetMapping("/")
	public String getLoginPage() 
	{
		return "login"; 
	}
	
	@GetMapping("/usernameInputForPasswordReset")
	public String getUsernameInputForPasswordResetPage()
	{
		return "UsernameInputForPasswordReset";
	}
	
	@PostMapping("/getPasswordResetMail")
	public ResponseEntity<String> RehandleGetPasswordResetMail(@RequestParam("email") String email, HttpSession session)
	{
		String to = email;
		String subject = "IntelliMate Password Reset Request";
		String body = "Dear User,\n\n"
				    + "We received a request to reset your password. Please click the link below to reset your password:\n"
				    + "http://localhost:8080/api/resetPassword\n\n"
				    + "If you did not request a password reset, please ignore this email.\n\n"
				    + "Best Regards,\n"
				    + "The IntelliMate Team";
		
		try 
		{
			systemMailService.sendEmail(to, subject, body);
		} 
		
		catch(Exception e) 
		{
			e.printStackTrace();
		} 
		
		
		session.setAttribute("Email", email);
		
		
		String htmlResponse = """
	            <html>
	                <body style="font-family: sans-serif; display: flex; justify-content: center; align-items: center; height: 100vh;">
	                    <div style="text-align: center; border: 1px solid #ddd; padding: 20px; border-radius: 10px;">
	                        <h2 style="color: #28a745;">Success!</h2>
	                        <p>A reset link has been sent to your registered email.</p>
	                        <a href="/api/" style="color: #4285f4; text-decoration: none;">Return to Login</a>
	                    </div>
	                </body>
	            </html>
	            """;

	    return ResponseEntity
	            .status(HttpStatus.OK)
	            .header("Content-Type", "text/html")
	            .body(htmlResponse);
	}
	
	// Serve reset password page
	@GetMapping("/resetPassword")
	public String getResetPasswordPage() 
	{
		return "ForgotPassword"; 
	}
	
	// Serve registration page
	@GetMapping("/registration")
	public String getRegistrationPage() 
	{
		return "registration"; 
	}
	
	@GetMapping("/isAuthenticated")
	public String isAuthenticated(Authentication authentication) 
	{
	    // Check if the user is logged in (either by valid JWT or by Remember-Me)
	    if(authentication != null && authentication.isAuthenticated() 
	        && !(authentication instanceof AnonymousAuthenticationToken)) 
	    {
	        
	        return "Dashboard"; // User is authenticated, serve dashboard
	    }

	    return "redirect:/api/"; // Redirect to login page
	}
	
	// Serve dashboard page
	@GetMapping("/dashboard")
	public String getDashboardPage(@RequestParam("token") String jwtToken,
								   @RequestParam(value = "rememberMe", defaultValue = "false") boolean rememberMe,
								   HttpServletRequest request,
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
		
		
		if(rememberMe || request.getSession().getAttribute("REMEMBER_ME_FLAG") != null)
		{
			// Extract user info from JWT
		    String userEmail = jwtTokenService.extractUserInfo(jwtToken);
		    
		    // Load the UserDetails from your UserService
		    UserDetails userDetails = userService.loadUserByUsername(userEmail);
		    
		    // Create an Authentication object for Spring Security to "see" the login
		    Authentication auth = new UsernamePasswordAuthenticationToken(
		            userDetails, null, userDetails.getAuthorities());

		    // Manually trigger Remember Me functionality
		    rememberMeService.loginSuccess(new HttpServletRequestWrapper(request) 
		    { @Override public String getParameter(String n) { return "true"; } }, response, auth);
		}
		
		return "Dashboard"; 
	}
	
	// Get the Google OAuth login URL
	@GetMapping("/google/login")
    public ResponseEntity<String> initiateGoogleLogin(@RequestParam(defaultValue = "false") boolean rememberMe, 
            										  HttpServletRequest request) 
	{
        try 
        {
            // Generate Google's authorization URL
            String authUrl = googleOAuthService.getAuthorizationUrl();
            
            // Store rememberMe flag in session for later use
            request.getSession().setAttribute("REMEMBER_ME_FLAG", rememberMe);
            
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
	            newUser.setGoogleTokenExpiry(googleUser.getGoogleTokenExpiry());
	            newUser.setCreatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	            newUser.setUpdatedAt(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	            newUser.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
	            newUser.setAuthMethod("google");
	            
	            userRepository.save(newUser);
	            
	   	            
	            // Generate JWT using new user ID
	            String jwtToken = jwtTokenService.generateToken(newUser.getEmail());
	            
	            
	            String to = googleUser.getEmail();
	    		String subject = "Welcome to IntelliMate!";
	    		String body = "Dear User,\n\n"
	    				    + "Welcome to IntelliMate! We're thrilled to have you on board.\n\n"
	    				    + "Best Regards,\n"
	    				    + "The IntelliMate Team";
	    		
	    		// send welcome mail
	    		try 
	    		{
	    			systemMailService.sendEmail(to, subject, body);
	    		} 
	    		
	    		catch(Exception e) 
	    		{
	    			e.printStackTrace();
	    		}
	            
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
	            existingUser.setGoogleTokenExpiry(googleUser.getGoogleTokenExpiry());
	            existingUser.setAuthMethod("both");  // Now supports both methods
	            
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
	
	// Check if user has linked Google account
	@GetMapping("/google/status")
    public ResponseEntity<Map<String, Boolean>> getGoogleStatus(@CookieValue(name = "jwt", required = false) String token) 
	{
		// If cookie missing or expired token → unauthorized
	    if(token == null || !jwtTokenService.isValid(token)) 
	    {
	        throw new RuntimeException("Missing or invalid JWT cookie");
	    }
	    
	    // Get user ID from token
	    String userID = jwtTokenService.extractUserInfo(token);
	    
	    // check if user has linked Google account
	    if(googleOAuthService.checkGoogleConnection(userID))
	    {
	    	return ResponseEntity.ok(Collections.singletonMap("isConnected", true));
	    }
	    
	    return ResponseEntity.ok(Collections.singletonMap("isConnected", false));
    }
	
    // Handle user registration
    @PostMapping("/UserRegistration")
    public ResponseEntity<Object> registerUser(@RequestParam("email") String email, @RequestParam("password") String password, 
    		                   				    HttpSession session) 
	{
    	String userName = email;
		String userPassword = password;
		String encryptedUserPassword = jasyptEncryptionService.encrypt(userPassword);
		
		User newUser = new User(userName, encryptedUserPassword, LocalDateTime.now(ZoneId.of("Asia/Kolkata")), 
								LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		
		newUser.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
		
		// Save user to database
		userRepository.save(newUser);
		
		// Create JWT token for the user
		String jwtToken = jwtTokenService.generateToken(userName);
		
		// send a mail to user welcoming them
		String to = userName;
		String subject = "Welcome to IntelliMate!";
		String body = "Dear User,\n\n"
				    + "Welcome to IntelliMate! We're thrilled to have you on board.\n\n"
				    + "Best Regards,\n"
				    + "The IntelliMate Team";
		
		try 
		{
			systemMailService.sendEmail(to, subject, body);
		} 
		
		catch(Exception e) 
		{
			e.printStackTrace();
		} 
		
		// Redirect to dashboard with JWT token
		String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
        
        return ResponseEntity.status(302)
            .header("Location", redirectUrl)
            .build();
	}
    
    // Handle user login
    @PostMapping("/UserLogin")
    public ResponseEntity<Object> userLogin(@RequestParam String email, 
    										@RequestParam String password,
    										HttpServletRequest request)
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
    			
    		// Set last login time
    		user.setLastLogin(LocalDateTime.now(ZoneId.of("Asia/Kolkata")));
    		
    		// see if "Remember Me" was checked
    		boolean rememberMe = request.getParameter("remember-me") != null;
    		  		
    		// Redirect to dashboard with JWT token and rememberMe flag
    		String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken + "&rememberMe=" + rememberMe;
    		
    	        
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
    
    @PostMapping("/handleResetPassword")
    public ResponseEntity<Object> handleResetPassword(@RequestParam("confirmPassword") String newPassword,
    												  HttpSession session)				
    {
    	// Get email from session
    	String email = session.getAttribute("Email").toString();
    	
    	User user = userRepository.findByEmail(email);
    	
    	if(user == null)
    	{
    		return ResponseEntity
    		       .status(HttpStatus.NOT_FOUND)
    		       .body("User not found. Please check the details and try again.");

    	}
    	
    	String encryptedNewPassword = jasyptEncryptionService.encrypt(newPassword);
    	
    	// Update user's password in database
    	user.setPassword(encryptedNewPassword);
    	
    	userRepository.save(user);
    	
    
    	// send a mail to user notifying password reset
		String to = email;
		String subject = "Your IntelliMate Password Has Been Reset";
		String body = "Dear User,\n\n"
				    + "Your password has been successfully reset. If you did not initiate this change, please contact our support team immediately.\n\n"
				    + "Best Regards,\n"
				    + "The IntelliMate Team";
		
		try 
		{
			systemMailService.sendEmail(to, subject, body);
		} 
		
		catch(Exception e) 
		{
			e.printStackTrace();
		} 

		// After successful password reset, generate a new JWT token
    	String jwtToken = jwtTokenService.generateToken(email);
    	
    	
    	
    	
    	System.out.println("Password reset successful for user: " + email + ", new password: " + newPassword);
    	
    	
    	
    	
    	
    	
    	// Redirect to dashboard with JWT token
    	String redirectUrl = "http://localhost:8080/api/dashboard?token=" + jwtToken;
		
		return ResponseEntity.status(302)
			.header("Location", redirectUrl)
			.build();
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
		
		// Validate message
		if(userMessage == null || userMessage.trim().isEmpty()) 
		{
			return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
		}
		
		try
		{
			Map<String, Object> context = new HashMap<>();
			context.put("userID", userID);
			context.put("sessionID", session.getId());
			
			
			// Send message and get AI response
			String AIResponse = aiEngine.chat(userMessage, context);

			
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
    public String logout(HttpSession session, 
    					 HttpServletRequest request,	
    					 HttpServletResponse response,
    					 Authentication authentication) 
	{
        // delete JWT cookie
        Cookie cookie = new Cookie("jwt", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        
        if(rememberMeService != null) 
		{
			// Invalidate Remember Me cookie
			rememberMeService.loginFail(request, response);
		}
        
        // Clear authentication info from SecurityContext
        SecurityContextHolder.clearContext();  

        // invalidate session
        session.invalidate();

        return "redirect:/api/";
    }
}



