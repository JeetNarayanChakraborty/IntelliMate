package com.IntelliMate.core.repository;

import org.springframework.stereotype.Component;




@Component
public class ContactInfo 
{
	private String name;
	private String email;
	
	
	
	public ContactInfo(String name, String email) 
	{
		this.name = name;
		this.email = email;
	}
	
	public String getName() 
	{
		return name;
	}
	
	public String getEmail() 
	{
		return email;
	}
	
	public void setName(String name) 
	{
		this.name = name;
	}
	
	public void setEmail(String email) 
	{
		this.email = email;
	}
}




