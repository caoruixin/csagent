package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.SessionOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionOutcomeRepository extends JpaRepository<SessionOutcome, String> {
}
