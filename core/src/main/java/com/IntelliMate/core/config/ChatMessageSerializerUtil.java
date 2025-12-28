package com.IntelliMate.core.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import java.util.List;



public class ChatMessageSerializerUtil 
{
    // Converts the List of messages to a single JSON string for JSONB column
    public static String messagesToJson(List<ChatMessage> messages) 
    {
        // LangChain4j provides a direct way to serialize a whole list
        return ChatMessageSerializer.messagesToJson(messages);
    }

    // Converts that JSON string back to a List so the LLM has its memory
    public static List<ChatMessage> messagesFromJson(String json) 
    {
        return ChatMessageDeserializer.messagesFromJson(json);
    }
}
