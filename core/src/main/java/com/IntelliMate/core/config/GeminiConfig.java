package com.IntelliMate.core.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;



// Configuration class for setting up the Gemini Model
@Configuration
public class GeminiConfig 
{
    @Bean
    public ChatLanguageModel chatLanguageModel(@Value("${gemini.api.key}") String apiKeyPath) 
    {
        try 
        {
            // Read the API key from the file path
            String apiKeyContent = Files.readString(Paths.get(apiKeyPath)).trim();

            // 2. Build the model
            return GoogleAiGeminiChatModel.builder()
                    .apiKey(apiKeyContent)
                    .modelName("gemini-2.5-flash")
                    .temperature(0.7)
                    .build();
        } 
        
        catch(IOException e) 
        {
            throw new RuntimeException("Failed to read Gemini API key from file: " + apiKeyPath, e);
        }
    }
}
