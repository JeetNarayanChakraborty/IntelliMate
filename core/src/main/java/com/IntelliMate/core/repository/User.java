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
	@Column(name = "email")
	private String email;
	
	@NotNull
	@Column(name = "password")
	private String password;
	
	@Column(name = "created_at")
	private LocalDateTime created_at;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
	private List<ConversationHistory> conversationHistories;
	
	
	
	public User() {}
	
	public User(String email, String password, LocalDateTime created_at) 
	{
		this.id = UUID.randomUUID().toString();
		this.email = email;
		this.password = password;
		this.created_at = created_at;
	}
	
	public String getId() 
	{
		return id;
	}

	public void setId(String id) 
	{
		this.id = id;
	}

	public String getPassword() 
	{
		return password;
	}
	
	public void setPassword(String password) 
	{
		this.password = password;
	}

	public String getEmail() 
	{
		return email;
	}

	public void setEmail(String email) 
	{
		this.email = email;
	}

	public LocalDateTime getCreated_at() 
	{
		return created_at;
	}

	public void setCreated_at(LocalDateTime created_at) 
	{
		this.created_at = created_at;
	}
}






