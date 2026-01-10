package com.IntelliMate.core.config;

import com.IntelliMate.core.PersistentChatMemoryStore;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;




@ExtendWith(MockitoExtension.class)
class GeminiConfig_Test 
{
    @Mock
    private PersistentChatMemoryStore chatMemoryStore;

    @InjectMocks
    private GeminiConfig geminiConfig;

    
    
    
    @Test
    @DisplayName("Verify successful ChatModel creation via external API key")
    void should_ReturnChatModel_When_ApiKeyFileIsReadable() 
    {
        org.slf4j.LoggerFactory.getLogger(this.getClass()); 

        String dummyPath = "/secrets/gemini.key";
        String dummyKey = "valid-google-gemini-key-123";

        try(MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS)) 
        {
            // Now we only stub the specific call we care about
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenReturn(dummyKey);

            ChatModel model = geminiConfig.chatLanguageModel(dummyPath);

            assertThat(model).isNotNull();
        }
    }

    @Test
    @DisplayName("Verify ChatMemoryProvider configuration and store association")
    void should_ReturnConfiguredChatMemoryProvider() 
    {
        // Act: Retrieval of the memory provider bean
        ChatMemoryProvider provider = geminiConfig.chatMemoryProvider();

        // Assert: Verification that the provider can generate memory instances
        assertThat(provider).isNotNull();
        
        // Corrected Method: Use .get() instead of .provide()
        ChatMemory chatMemory = provider.get("test-memory-id");
        assertThat(chatMemory).isNotNull();
    }

    @Test
    @DisplayName("Verify failure handling when API key file is missing")
    void should_ThrowRuntimeException_When_ApiKeyFileReadFails() 
    {
        // Arrange: Setup of an inaccessible file scenario
        String invalidPath = "invalid/path/key.txt";

        // Act & Assert: Confirmation of fatal error propagation
        try(MockedStatic<Files> mockedFiles = mockStatic(Files.class)) 
        {
            mockedFiles.when(() -> Files.readString(any(Path.class))).thenThrow(new IOException("Disk error"));
            assertThrows(RuntimeException.class, () -> geminiConfig.chatLanguageModel(invalidPath));
        }
    }
}








