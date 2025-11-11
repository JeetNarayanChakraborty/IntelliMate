package com.IntelliMate.core.service.JWTService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Date;



@Service
public class JWTTokenService 
{  
    // Generate a secure key
    private final SecretKey SECRET_KEY;
    
    
    public JWTTokenService() 
	{
    	KeyGenerator keyGen=null;
    	
		try 
		{
			keyGen = KeyGenerator.getInstance("HmacSHA256");
		} 
		catch(NoSuchAlgorithmException e) {};
		
        keyGen.init(256); // 256-bit key
        
    	this.SECRET_KEY = keyGen.generateKey();
	}
    
    // Generate JWT token
    public String generateToken(String userID) 
    {
        return Jwts.builder()
            .subject(userID)     // Store user ID
            .issuedAt(new Date())     // Current time
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))  // 7 days
            .signWith(SECRET_KEY)     // Sign with key (no need to specify algorithm)
            .compact();
    }
    
    // Extract email from token
    public String extractUserID(String token) 
    {
        Claims claims = Jwts.parser()
            .verifyWith(SECRET_KEY)                             // Updated API
            .build()
            .parseSignedClaims(token)
            .getPayload();
        
        return claims.getSubject();
    }
    
    // Validate token
    public boolean isValid(String token) 
    {
        try 
        {
            Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token);
            return true;
        } 
        
        catch(Exception e) 
        {
            return false;
        }
    }
    
    // Check if token is expired
    public boolean isExpired(String token) 
    {
        try 
        {
            Claims claims = Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
            
            return claims.getExpiration().before(new Date());
        } 
        
        catch(Exception e) 
        {
            return true;
        }
    }
}




