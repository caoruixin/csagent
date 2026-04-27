package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.LlmCallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmCallLogRepository extends JpaRepository<LlmCallLog, Long> {

    List<LlmCallLog> findBySessionIdOrderByCreatedAt(String sessionId);

    List<LlmCallLog> findBySessionIdAndCallType(String sessionId, String callType);
}
