package com.IntelliMate.core.repository;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
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
	@Column(name = "name")
	private String name;
	
	@NotNull
	@Email
	@Column(name = "email")
	private String email;
	
	@Column(name = "created_at")
	private String created_at;
	
	@OneToOne(cascade = CascadeType.REMOVE)
	@JoinColumn(name = "conversation_histories")
	private ConversationHistory conversationHistory;

	
	
	public User(String name, String email, String created_at, ConversationHistory conversationHistory) 
	{
		this.id = UUID.randomUUID().toString();
		this.name = name;
		this.email = email;
		this.created_at = created_at;
		this.conversationHistory = conversationHistory;
	}
	
	public String getId() 
	{
		return id;
	}

	public void setId(String id) 
	{
		this.id = id;
	}

	public String getName() 
	{
		return name;
	}

	public void setName(String name) 
	{
		this.name = name;
	}

	public String getEmail() 
	{
		return email;
	}

	public void setEmail(String email) 
	{
		this.email = email;
	}

	public String getCreated_at() 
	{
		return created_at;
	}

	public void setCreated_at(String created_at) 
	{
		this.created_at = created_at;
	}

	public ConversationHistory getConversationHistory() 
	{
		return conversationHistory;
	}

	public void setConversationHistory(ConversationHistory conversationHistory) 
	{
		this.conversationHistory = conversationHistory;
	}
}






