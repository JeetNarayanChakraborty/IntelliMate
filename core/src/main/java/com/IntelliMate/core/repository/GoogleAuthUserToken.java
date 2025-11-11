package com.IntelliMate.core.repository;

import org.checkerframework.common.aliasing.qual.Unique;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;



@Entity
@Table(name = "google_auth_user_token")
public class GoogleAuthUserToken 
{
	@Id
	@Unique
	@Column(name = "user_id", unique = true)
	private String userId;
	
	@Column(name = "access_token")
	private String accessToken;
	
	@Column(name = "refresh_token")
	private String refreshToken;
	
	@Column(name = "token_expiry")
	private LocalDateTime tokenExpiry;
	
	@Column(name = "created_at")
	private LocalDateTime createdAt;
	
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;
	
	
	
	public GoogleAuthUserToken( ) {}
	
	public GoogleAuthUserToken(String accessToken, String refreshToken, LocalDateTime tokenExpiry, LocalDateTime createdAt, LocalDateTime updatedAt ) 
	{
		this.userId = UUID.randomUUID().toString();
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.tokenExpiry = tokenExpiry;
	}
	
	public String getUserId() 
	{
		return userId;
	}

	public String getAccessToken() 
	{
		return accessToken;
	}

	public void setAccessToken(String accessToken) 
	{
		this.accessToken = accessToken;
	}

	public String getRefreshToken() 
	{
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) 
	{
		this.refreshToken = refreshToken;
	}

	public LocalDateTime getTokenExpiry() 
	{
		return tokenExpiry;
	}

	public void setTokenExpiry(LocalDateTime tokenExpiry) 
	{
		this.tokenExpiry = tokenExpiry;
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
}














