package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.BotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotSessionRepository extends JpaRepository<BotSession, String> {

    List<BotSession> findByHandlingState(String handlingState);
}
