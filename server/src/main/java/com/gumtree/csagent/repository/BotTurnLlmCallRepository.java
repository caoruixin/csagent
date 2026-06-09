package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.BotTurnLlmCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BotTurnLlmCallRepository extends JpaRepository<BotTurnLlmCall, Long> {

    List<BotTurnLlmCall> findByBotTurnIdOrderByStepIndex(String botTurnId);
}
