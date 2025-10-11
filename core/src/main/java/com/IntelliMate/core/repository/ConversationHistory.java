package com.IntelliMate.core.repository;

import java.time.LocalDateTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
	@Column(name = "user_id")
	private String user_id;
	
	@NotNull
	@Column(name = "message")
	private String message;
	
	@NotNull
	@Column(name = "response")
	private String response;
	
	@Column(name = "timestamp")
	private LocalDateTime timestamp;
	
	
	
	public ConversationHistory(String user_id, String message, String response, LocalDateTime timestamp) 
	{
		this.id = UUID.randomUUID().toString();
		this.user_id = user_id;
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

	public String getUser_id() 
	{
		return user_id;
	}

	public void setUser_id(String user_id) 
	{
		this.user_id = user_id;
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
}







