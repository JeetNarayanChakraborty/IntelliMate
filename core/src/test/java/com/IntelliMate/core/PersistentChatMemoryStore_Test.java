package com.IntelliMate.core;

import com.IntelliMate.core.repository.ChatSession;
import com.IntelliMate.core.repository.ChatSessionRepository;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;




@ExtendWith(MockitoExtension.class)
class PersistentChatMemoryStore_Test 
{
    @Mock private ChatSessionRepository sessionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private PersistentChatMemoryStore chatMemoryStore;

    private final String testUserEmail = "test@intellimate.com";
    private final String testSessionId = "session_123";
    private final String userCredential = testUserEmail + ":" + testSessionId;

    
    
    
    @Test
    @DisplayName("Verify retrieval of existing chat messages")
    void should_ReturnMessages_When_SessionExists() 
    {
        // Arrange: Use the correct LangChain4j JSON format
        // [{"type":"USER","contents":[{"type":"TEXT","text":"Hello"}]}]
        ChatSession existingSession = new ChatSession();
        String validJson = "[{\"type\":\"USER\",\"contents\":[{\"type\":\"TEXT\",\"text\":\"Hello\"}]}]";
        existingSession.setMemoryData(validJson);
        
        when(sessionRepository.findBySessionId(testSessionId)).thenReturn(Optional.of(existingSession));

        // Act
        List<ChatMessage> messages = chatMemoryStore.getMessages(userCredential);

        // Assert
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        
        // Additional verification to ensure the text was parsed correctly
        UserMessage userMessage = (UserMessage) messages.get(0);
        assertThat(userMessage.singleText()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("Verify update logic for new session initialization")
    void should_CreateNewSession_When_SessionNotFound() 
    {
        // Arrange: Preparation for a first-time session scenario
        List<ChatMessage> messages = Arrays.asList(new UserMessage("Hi"), new AiMessage("Hello"));
        User mockUser = new User();
        mockUser.setEmail(testUserEmail);

        when(sessionRepository.findBySessionId(testSessionId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(testUserEmail)).thenReturn(mockUser);

        // Act: Execution of the update operation
        chatMemoryStore.updateMessages(userCredential, messages);

        // Assert: Verification that a new session was persisted with correctly mapped user
        verify(sessionRepository).save(argThat(session -> 
            session.getSessionId().equals(testSessionId) && 
            session.getUser().getEmail().equals(testUserEmail)
        ));
    }

    @Test
    @DisplayName("Verify update logic for existing session data")
    void should_UpdateExistingSession_When_SessionExists() 
    {
        // Arrange: Setup of an existing session record
        ChatSession existingSession = spy(new ChatSession());
        List<ChatMessage> messages = List.of(new UserMessage("Update me"));
        
        when(sessionRepository.findBySessionId(testSessionId)).thenReturn(Optional.of(existingSession));

        // Act: Execution of the message update
        chatMemoryStore.updateMessages(userCredential, messages);

        // Assert: Confirmation of data overwrite and timestamp update
        verify(existingSession).setMemoryData(anyString());
        verify(sessionRepository).save(existingSession);
    }

    @Test
    @DisplayName("Verify deletion of session memory")
    void should_DeleteMessages_When_IdProvided() 
    {
        // Act: Execution of the deletion request
        chatMemoryStore.deleteMessages(testSessionId);

        // Assert: Confirmation of repository interaction
        verify(sessionRepository).deleteById(testSessionId);
    }
}







