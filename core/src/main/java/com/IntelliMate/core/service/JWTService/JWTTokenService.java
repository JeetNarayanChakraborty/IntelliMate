package com.IntelliMate.core.service.JWTService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Date;
import java.nio.file.Files;
import java.nio.file.Paths;



@Service
public class JWTTokenService 
{  
    private final SecretKey SECRET_KEY;
    
    
    public JWTTokenService(@Value("${jwt.secret.key}") String jwtSecretKeyPath) 
	{
    	try
    	{
    		String keyString=null;
        	
    		keyString = Files.readString(Paths.get(jwtSecretKeyPath)).trim();

    		// Load the key from the file
    		byte[] keyBytes;
    		
    		keyBytes = java.util.Base64.getDecoder().decode(keyString); 
    		
    		SECRET_KEY = new SecretKeySpec(keyBytes, "HmacSHA256");
    	}
    	
    	catch(Exception e) 
		{
    		throw new RuntimeException("Failed to load JWT secret key", e);
		}
	}
    
    // Generate JWT token
    public String generateToken(String user_identifier) 
    {
        return Jwts.builder()
            .subject(user_identifier)     // Store user ID
            .issuedAt(new Date())     // Current time
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7))  // 7 days
            .signWith(SECRET_KEY)     // Sign with key (no need to specify algorithm)
            .compact();
    }
    
    // Extract user info from token
    public String extractUserInfo(String token) 
    {
        Claims claims = Jwts.parser()
            .verifyWith(SECRET_KEY)                   
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




