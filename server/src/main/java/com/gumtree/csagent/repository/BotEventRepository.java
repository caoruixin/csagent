package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.BotEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotEventRepository extends JpaRepository<BotEvent, String> {

    List<BotEvent> findBySessionIdOrderByCreatedAt(String sessionId);

    List<BotEvent> findByEventType(String eventType);
}
