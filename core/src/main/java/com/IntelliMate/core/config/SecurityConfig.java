package com.IntelliMate.core.config;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.context.annotation.Lazy;




@Configuration
@EnableWebSecurity
public class SecurityConfig 
{	
	@Bean(name = "rememberMeKey")
    public String rememberMeSecretKey(@Value("${remember.me.secret.key}") String path) 
    {
        try 
        {
            return Files.readString(Paths.get(path)).trim();
        } 
        
        catch(Exception e) 
        {
            System.err.println("ERROR: Could not read secret key file");
            return "emergency-fallback-key-12345";
        }
    }
	
	@Bean
	public RememberMeServices rememberMeServices(@Qualifier("rememberMeKey") String rememberMeSecretKey,
	                                             @Lazy UserDetailsService userService) 
	{
	    TokenBasedRememberMeServices service = new TokenBasedRememberMeServices(rememberMeSecretKey, userService);
	    service.setTokenValiditySeconds(1209600);   // 2 weeks
	    service.setCookieName("remember-me-cookie"); 
	    service.setParameter("remember-me");     
	    return service;
	}
	
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, 
    											   RememberMeServices rememberMeServices) throws Exception 
    {	
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                    .anyRequest().permitAll())
                
            
             // Configure Remember Me functionality
            .rememberMe(remember -> remember
                    .rememberMeServices(rememberMeServices));
    
        return http.build();
    }
}



