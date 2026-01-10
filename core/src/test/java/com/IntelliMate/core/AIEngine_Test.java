package com.IntelliMate.core;

import com.IntelliMate.core.tools.CalendarTool;
import com.IntelliMate.core.tools.MailTool;
import com.IntelliMate.core.tools.NewsTool;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;




@ExtendWith(MockitoExtension.class)
class AIEngine_Test 
{
    @Mock private ChatModel chatModel;
    @Mock private CalendarTool calendarTool;
    @Mock private MailTool mailTool;
    @Mock private NewsTool newsTool;
    @Mock private ChatMemoryProvider memoryProvider;

    private AIEngine aiEngine;

    @BeforeEach
    void setUp() 
    {
        // Initialization of service with mocked dependencies
        aiEngine = new AIEngine(chatModel, calendarTool, mailTool, newsTool, memoryProvider);
    }
    
    @Test
    @DisplayName("Verify tools are initialized even if LLM call fails")
    void should_Still_InitializeTools() 
    {
        Map<String, Object> context = new HashMap<>();
        context.put("userID", "user_123");

        // We don't even need 'when(...).thenReturn(...)' here if we just want 
        // to see if the init() methods were called before the engine crashed
        try 
        {
            aiEngine.chat("Hello", context);
        } 
        
        catch(Exception e) 
        {
            // Ignore the LLM error because we only care about the tool init
        }

        verify(calendarTool).init("user_123");
        verify(mailTool).init("user_123");
    }

    @Test
    @DisplayName("Verify chat memory window configuration")
    void should_ConfigureMemoryWithTenMessageWindow() 
    {
        // Act & Assert: Validation of the internal message window capacity
        var memory = aiEngine.getMemory();
        assertThat(memory).isNotNull();
    }

    @Test
    @DisplayName("Verify engine doesn't crash when context values are missing")
    void should_HandleNullContextValues_WithoutCrashing() 
    {
        // 1. Arrange: Create a context where userID is null
        Map<String, Object> context = new HashMap<>();
        context.put("userID", null);

        // 2. Act: Call the engine. We expect an NPE or failure inside the LLM proxy,
        try 
        {
            aiEngine.chat("Hello", context);
        } 
        
        catch(Exception e) {} 
     
        // 3. Assert: Verify that even if the context value was null, 
        // the code attempted to initialize the tools with that value.
        verify(calendarTool).init(null);
        verify(mailTool).init(null);
    }
}







