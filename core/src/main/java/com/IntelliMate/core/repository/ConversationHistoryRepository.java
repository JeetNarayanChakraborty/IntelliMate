package com.IntelliMate.core.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;



@Repository
public interface ConversationHistoryRepository extends JpaRepository<ConversationHistory, String>
{
	public ConversationHistory findByUser_id(String id);
	public void deleteByUser_id(String id);
	
	@Query(value = "SELECT * FROM conversation_history WHERE user_id = :id ORDER BY timestamp DESC LIMIT :n", nativeQuery = true)
	public List<ConversationHistory> getLastNConversationByUser_id(int n, String id);
}
