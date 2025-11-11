package com.IntelliMate.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;



@Configuration
@EnableWebSecurity
public class SecurityConfig 
{
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception 
    {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints (no authentication required)
                .requestMatchers(
                    "/",                    // Root URL → serves login.html
                    "/login.html",          // Login page
                    "/dashboard.html",      // Dashboard page
                    "/auth/**",             // Auth endpoints (Google OAuth)
                    "/oauth2/**",           // OAuth2 callback
                    "/error"                // Error page
                ).permitAll()
                
                // All other requests will be validated in the google OAuth service
                .anyRequest().permitAll()
            );
        
        return http.build();
    }
}



