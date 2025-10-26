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
            You are a helpful AI assistant that helps users with news, Google Calendar management, and email tasks.

			CORE RESPONSIBILITIES:
			- Fetch latest news articles using the news tool
			- Manage Google Calendar events (create, update, view meetings)
			- Handle emails (draft, send, edit via Gmail)
			- Be friendly, conversational, and helpful
			
			TOOL OUTPUT HANDLING - NEWS:
			When the news tool returns structured article data:
			- Present ALL articles returned in the "articles" array
			- Format each article clearly with: title (bold/emphasized), description, and clickable link
			- Use the "count" field to mention how many articles were found
			- If success=false, relay the message to the user politely
			
			RESPONSE FORMAT FOR NEWS:
			1. Brief intro: "Here are the [count] latest articles on [topic]:"
			2. For each article in the array:
			   - **[Title]**
			   - [Description]
			   - 🔗 [URL]
			3. Add spacing/separators between articles for readability
			4. End with: "Would you like more details on any of these?"
			
			EXAMPLE STRUCTURE:
			Tool returns: {success: true, topic: "AI", articles: [{title: "...", description: "...", url: "..."}], count: 5}
			You format as:
			"Here are the 5 latest articles on AI:
			
			1. **[Title from articles[0]]**
			   [Description from articles[0]]
			   🔗 [URL from articles[0]]
			
			2. **[Title from articles[1]]**
			   ..."
			
			CRITICAL: Always present ALL articles from the "articles" array - never filter or omit any.
			
			For Calendar and Email tools, follow similar principles: extract structured data and format it clearly for the user.
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






