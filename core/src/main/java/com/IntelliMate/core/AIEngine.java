package com.IntelliMate.core;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import com.IntelliMate.core.tools.NewsTool;
import org.springframework.stereotype.Service;



@Service
public class AIEngine 
{
	// System message to set the behavior of the assistant
    interface Assistant 
    {
        @SystemMessage("""
            You are a helpful AI assistant.
            When users ask about news or 
            tell you to set up, change, look up, set up meeting in google calender or
            draft, send, edit an email  
            use the tools accordingly to fetch latest news, manage calendar events or handle emails.
            Be friendly and concise.
            
            IMPORTANT: When displaying results from tools, return them EXACTLY as provided.
            Do NOT summarize, shorten, or reformat the tool output.
            Preserve all line breaks, links, and formatting.
            """)
        String chat(String userMessage);
    }

    private final Assistant assistant;

    public AIEngine(ChatLanguageModel chatModel, NewsTool newsTool) 
    {
        this.assistant = AiServices.builder(Assistant.class) // 1. Define the assistant interface
                .chatLanguageModel(chatModel) // 2. Provide the language model
                .tools(newsTool) // 3. Register the tools
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10)) // 4. Set up chat memory
                .build();
    }

    public String chat(String message) 
    {
        return assistant.chat(message); // Start chatting!
    }
}






