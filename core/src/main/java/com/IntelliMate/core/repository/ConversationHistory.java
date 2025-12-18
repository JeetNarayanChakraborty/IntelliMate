package com.IntelliMate.core.repository;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;




@Entity
@Table(name = "conversation_history")
public class ConversationHistory 
{
	@Id
	@NotNull
	@Column(name = "id")
	private String id;
	
	@NotNull
	@Column(name = "message")
	private String message;
	
	@NotNull
	@Column(name = "response")
	private String response;
	
	@Column(name = "timestamp")
	private LocalDateTime timestamp;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "email", nullable = false)
	private User user;
	
	
	
	
	public ConversationHistory() {}
	
	public ConversationHistory(String message, String response, LocalDateTime timestamp) 
	{
		this.id = UUID.randomUUID().toString();
		this.message = message;
		this.response = response;
		this.timestamp = timestamp;
	}

	public String getId() 
	{
		return id;
	}

	public void setId(String id) 
	{
		this.id = id;
	}

	public String getMessage() 
	{
		return message;
	}

	public void setMessage(String message) 
	{
		this.message = message;
	}

	public String getResponse() 
	{
		return response;
	}

	public void setResponse(String response) 
	{
		this.response = response;
	}

	public LocalDateTime getTimestamp() 
	{
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) 
	{
		this.timestamp = timestamp;
	}
	
	public void setUser(User user) 
	{
		this.user = user;
	}
}







