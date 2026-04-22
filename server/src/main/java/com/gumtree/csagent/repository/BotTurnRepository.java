package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.BotTurn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotTurnRepository extends JpaRepository<BotTurn, String> {

    List<BotTurn> findBySessionIdOrderByTurnIndex(String sessionId);
}
