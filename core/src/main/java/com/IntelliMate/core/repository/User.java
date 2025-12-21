package com.IntelliMate.core.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;



@Entity
@Table(name = "users")
public class User
{
    @Id
    @NotNull
    @Column(name = "id")
    private String id;
    
    @NotNull
    @Email
    @Column(name = "email", unique = true)
    private String email;
    
    // Nullable for Google-only users
    @Column(name = "password")
    private String password;
    
    // Google OAuth fields (nullable for manual-only users)
    @Column(name = "google_id", unique = true)
    private String googleId;
    
    @Column(name = "google_access_token", length = 2000)
    private String googleAccessToken;
    
    @Column(name = "google_refresh_token", length = 2000)
    private String googleRefreshToken;
    
    @Column(name = "google_token_expiry")
    private LocalDateTime googleTokenExpiry;
    
    // Authentication method: "manual", "google", or "both"
    @Column(name = "auth_method", length = 20)
    private String authMethod;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ConversationHistory> conversationHistories;
    
    

    public User() {}
    
    // Constructor for manual registration
    public User(String email, String password, LocalDateTime createdAt) 
    {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.password = password;
        this.authMethod = "manual";
        this.createdAt = createdAt;
        this.updatedAt = LocalDateTime.now();
    }
    
    // Constructor for Google registration
    public User(String email, String googleId, String googleAccessToken, 
                String googleRefreshToken, long googleTokenExpiry) 
    {
        this.id = UUID.randomUUID().toString();
        this.email = email;
        this.googleId = googleId;
        this.googleAccessToken = googleAccessToken;
        this.googleRefreshToken = googleRefreshToken;
        this.googleTokenExpiry = LocalDateTime.now().plusSeconds(googleTokenExpiry);
        this.authMethod = "google";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    
    public String getId() 
    {
        return id;
    }
    
    public void setId(String id) 
    {
        this.id = id;
    }
    
    public String getEmail() 
    {
        return email;
    }
    
    public void setEmail(String email) 
    {
        this.email = email;
    }
    
    public String getPassword() 
    {
        return password;
    }
    
    public void setPassword(String password) 
    {
        this.password = password;
    }
    
    public String getGoogleId() 
    {
        return googleId;
    }
    
    public void setGoogleId(String googleId) 
    {
        this.googleId = googleId;
    }
    
    public String getGoogleAccessToken() 
    {
        return googleAccessToken;
    }
    
    public void setGoogleAccessToken(String googleAccessToken) 
    {
        this.googleAccessToken = googleAccessToken;
    }
    
    public String getGoogleRefreshToken() 
    {
        return googleRefreshToken;
    }
    
    public void setGoogleRefreshToken(String googleRefreshToken) 
    {
        this.googleRefreshToken = googleRefreshToken;
    }
    
    public LocalDateTime getGoogleTokenExpiry() 
    {
        return googleTokenExpiry;
    }
    
    public void setGoogleTokenExpiry(LocalDateTime googleTokenExpiry) 
    {
        this.googleTokenExpiry = googleTokenExpiry;
    }
    
    public String getAuthMethod() 
    {
        return authMethod;
    }
    
    public void setAuthMethod(String authMethod) 
    {
        this.authMethod = authMethod;
    }
    
    public LocalDateTime getCreatedAt() 
    {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) 
    {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() 
    {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) 
    {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getLastLogin() 
    {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) 
    {
        this.lastLogin = lastLogin;
    }
    
    public List<ConversationHistory> getConversationHistories() 
    {
        return conversationHistories;
    }
    
    public void setConversationHistories(List<ConversationHistory> conversationHistories) 
    {
        this.conversationHistories = conversationHistories;
    }
}









