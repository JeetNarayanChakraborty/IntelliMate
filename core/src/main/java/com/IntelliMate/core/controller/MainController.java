package com.IntelliMate.core.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import com.IntelliMate.core.AIEngine;



@RestController
@RequestMapping("/api")
public class MainController 
{
	private final AIEngine aiEngine;
	
	public MainController(AIEngine aiEngine) 
	{
		this.aiEngine = aiEngine;
	}
	
	@GetMapping("/chat")
	public ResponseEntity<Map<String, String>> chat (@RequestParam String query) 
	{
		String userMessage = query;

		if(userMessage == null || userMessage.trim().isEmpty()) 
		{
			return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
		}
		
		try
		{
			String AIResponse = aiEngine.chat(userMessage);
			
			System.out.println("AIResponse: " + AIResponse);
			
			
			
			
			return ResponseEntity.ok(Map.of("response", AIResponse));
		}
		
		catch(Exception e)
		{
			return ResponseEntity.status(500).body(Map.of("error", "Internal server error: " + e.getMessage()));
		}
	}
}



