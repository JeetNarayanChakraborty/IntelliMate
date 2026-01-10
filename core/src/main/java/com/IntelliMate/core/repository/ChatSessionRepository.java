package com.IntelliMate.core.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String>
{
	Optional<ChatSession> findBySessionId(String sessionId);
	void deleteBySessionId(String sessionId);
	String findUserIdBySessionId(String sessionId);
}
