package com.IntelliMate.core.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;




class ChatMessageSerializerUtil_Test 
{

    @Test
    @DisplayName("Verify conversion of message list to JSON string")
    void should_ConvertMessagesToJson_When_ListProvided() 
    {
        // Arrange: Creation of a mixed list of chat messages
        List<ChatMessage> messages = Arrays.asList(
                new UserMessage("Hello Bot"),
                new AiMessage("Hello User")
        );

        // Act: Execution of the serialization logic
        String json = ChatMessageSerializerUtil.messagesToJson(messages);

        // Assert: Confirmation of non-empty JSON output containing message content
        assertThat(json).isNotBlank();
        assertThat(json).contains("Hello Bot", "Hello User");
    }

    @Test
    @DisplayName("Verify reconstruction of message list from JSON string")
    void should_ConvertJsonToMessages_When_ValidJsonProvided() 
    {
        // Arrange: Setup of a standard LangChain4j serialized JSON string
        String json = "[{\"type\":\"USER\",\"contents\":[{\"type\":\"TEXT\",\"text\":\"Test Query\"}]}]";

        // Act: Execution of the deserialization logic
        List<ChatMessage> result = ChatMessageSerializerUtil.messagesFromJson(json);

        // Assert: Validation of the resulting object type and content extraction
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(UserMessage.class);
        
        // Fix: Extraction of text from the UserMessage content list
        UserMessage userMessage = (UserMessage) result.get(0);
        String extractedText = ((TextContent) userMessage.contents().get(0)).text();
        assertThat(extractedText).isEqualTo("Test Query");
    }

    @Test
    @DisplayName("Verify integrity of the full serialization-deserialization roundtrip")
    void should_MaintainDataIntegrity_DuringRoundTrip() 
    {
        // Arrange: Preparation of original state
        List<ChatMessage> originalMessages = List.of(new UserMessage("Round trip test"));

        // Act: Execution of full cycle (Object -> JSON -> Object)
        String json = ChatMessageSerializerUtil.messagesToJson(originalMessages);
        List<ChatMessage> reconstructedMessages = ChatMessageSerializerUtil.messagesFromJson(json);

        // Assert: Verification that the final state matches the initial state content
        assertThat(reconstructedMessages).hasSize(1);
        
        UserMessage original = (UserMessage) originalMessages.get(0);
        UserMessage reconstructed = (UserMessage) reconstructedMessages.get(0);
        
        // Comparison of text content across the round trip
        assertThat(((TextContent) reconstructed.contents().get(0)).text())
                .isEqualTo(((TextContent) original.contents().get(0)).text());
    }
}






