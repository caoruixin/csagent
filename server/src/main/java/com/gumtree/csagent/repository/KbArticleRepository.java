package com.gumtree.csagent.repository;

import com.gumtree.csagent.model.KbArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface KbArticleRepository extends JpaRepository<KbArticle, String> {

    List<KbArticle> findByIsPublishedTrue();

    /**
     * Look articles up by their customer-visible canonical URL. Used by
     * {@code ArticleCardAssembler} to map a URL the bot cited in its reply
     * back to the KB row it came from, on turns where the reply ran no
     * retrieval tool of its own and therefore has no per-turn candidate set to
     * map against. Unpublished rows are excluded at the query level so
     * draft / suspended content can never reach a customer-facing card.
     */
    List<KbArticle> findBySourceUrlInAndIsPublishedTrue(Collection<String> sourceUrls);
}
