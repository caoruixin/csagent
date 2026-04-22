package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.MockHandoverLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockHandoverLogRepository extends JpaRepository<MockHandoverLog, String> {

    List<MockHandoverLog> findBySessionId(String sessionId);
}
