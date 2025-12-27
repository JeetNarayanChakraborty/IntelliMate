package com.IntelliMate.core.repository;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UpdateTimestamp;




@Entity
@Table(name = "chat_session")
public class ChatSession 
{
	@Id
	@NotNull
    private String sessionId;

    @Column
    @NotNull
    private String userId;

    @Column(columnDefinition = "jsonb")
    @NotNull
    private String memoryData;

    @UpdateTimestamp
    @NotNull
    private LocalDateTime lastUpdatedAt;
    
    
    
    public ChatSession() {}
    
    public ChatSession(String sessionId, String userId, String memoryData) 
	{
		this.sessionId = sessionId;
		this.userId = userId;
		this.memoryData = memoryData;
		this.lastUpdatedAt = LocalDateTime.now();
	}

	public String getSessionId() 
	{
		return sessionId;
	}

	public void setSessionId(String sessionId) 
	{
		this.sessionId = sessionId;
	}

	public String getUserId() 
	{
		return userId;
	}

	public void setUserId(String userId) 
	{
		this.userId = userId;
	}

	public String getMemoryData() 
	{
		return memoryData;
	}

	public void setMemoryData(String memoryData) 
	{
		this.memoryData = memoryData;
	}

	public LocalDateTime getLastUpdatedAt() 
	{
		return lastUpdatedAt;
	}

	public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) 
	{
		this.lastUpdatedAt = lastUpdatedAt;
	}
}









