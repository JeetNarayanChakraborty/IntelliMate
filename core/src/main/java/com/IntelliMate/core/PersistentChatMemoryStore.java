package com.IntelliMate.core;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.IntelliMate.core.repository.ChatSession;
import com.IntelliMate.core.repository.ChatSessionRepository;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import com.IntelliMate.core.config.ChatMessageSerializerUtil;
import com.IntelliMate.core.repository.User;
import com.IntelliMate.core.repository.UserRepository;





@Service
public class PersistentChatMemoryStore implements ChatMemoryStore
{
	@Autowired
    private ChatSessionRepository sessionRepository;
	
	@Autowired
	private UserRepository userRepository;
	
	
	
    @Override
    public List<ChatMessage> getMessages(Object userCredential) 
    {
    	String[] parts = userCredential.toString().split(":");
    	String sessionId = parts[0];
    	
        return sessionRepository.findBySessionId(sessionId.toString())
                .map(session -> ChatMessageSerializerUtil.messagesFromJson(session.getMemoryData()))
                .orElse(new ArrayList<>());
    }

    @Override
    public void updateMessages(Object userCredential, List<ChatMessage> messages) 
    {
    	String[] parts = userCredential.toString().split(":");
    	String sessionId = parts[0];
    	String userID = parts[1];
    	
        // Check if this is the first time we see this session
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseGet(() -> 
                {
                    ChatSession newSession = new ChatSession();
                    newSession.setSessionId(sessionId);
                    newSession.setLastUpdatedAt(LocalDateTime.now());
                    User user = userRepository.findByEmail(userID);
                    newSession.setUser(user);
                    
                    return newSession; 
                });

		// if exists, update the memory data
        session.setMemoryData(ChatMessageSerializerUtil.messagesToJson(messages));

        sessionRepository.save(session);
    }

    @Override
    public void deleteMessages(Object sessionId) 
    {
    	sessionRepository.deleteById(sessionId.toString());
    }
}







