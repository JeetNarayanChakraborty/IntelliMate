package com.IntelliMate.core.repository;

import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;




@Entity
@Table(name = "chat_sessions")
public class ChatSession 
{
	@Id
	@NotNull
	@Column(name = "session_id")
    private String sessionId;

	@NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "email")
    private User user;

	@JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "memory_data", columnDefinition = "jsonb")
    private String memoryData;
	
	@NotNull
	@Column(name = "created_at")
	private LocalDateTime createdAt;

    @NotNull
    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;
    
    
    
    
    
    public ChatSession() {}
    
    public ChatSession(String sessionId, String userId, String memoryData, User user) 
	{
		this.sessionId = sessionId;
		this.user = user;
		this.memoryData = memoryData;
		this.createdAt = LocalDateTime.now();
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
	
	public User getUser() 
	{
		return user;
	}
	
	public void setUser(User user) 
	{
		this.user = user;
	}
	
	public LocalDateTime getCreatedAt() 
	{
		return createdAt;
	}
	
	public void setCreatedAt(LocalDateTime createdAt) 
	{
		this.createdAt = createdAt;
	}
}









