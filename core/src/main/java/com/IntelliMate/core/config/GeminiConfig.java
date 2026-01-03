package com.IntelliMate.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.IntelliMate.core.PersistentChatMemoryStore;

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;



// Configuration class for setting up the Gemini Model and Chat Memory Provider
@Configuration
public class GeminiConfig 
{
    @Autowired
    private PersistentChatMemoryStore chatMemoryStore;
  
    
	
    @Bean
    public ChatModel chatLanguageModel(@Value("${gemini.api.key}") String apiKeyPath) 
    {
        try 
        {
            // Read the API key from the file path
            String geminiApiKey = Files.readString(Paths.get(apiKeyPath)).trim();

            // 2. Build the model
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName("gemini-2.5-flash-lite")
                    .logRequestsAndResponses(true)
                    .temperature(0.7)
                    .allowCodeExecution(false)
                    .build();
        } 
        
        catch(IOException e) 
        {
            throw new RuntimeException("Failed to read Gemini API key from file: " + apiKeyPath, e);
        }
    }
    
    @Bean
    public ChatMemoryProvider chatMemoryProvider() 
    {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(20) 
                .chatMemoryStore(chatMemoryStore)
                .build();
    }
}


















