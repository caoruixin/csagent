package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KbArticleRepository extends JpaRepository<KbArticle, String> {

    List<KbArticle> findByIsPublishedTrue();
}
