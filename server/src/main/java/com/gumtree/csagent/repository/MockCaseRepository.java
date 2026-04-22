package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.MockCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockCaseRepository extends JpaRepository<MockCase, String> {

    List<MockCase> findBySessionId(String sessionId);
}
