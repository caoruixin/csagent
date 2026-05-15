package com.gumtree.csagent.service.knowledge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sprint 14 §L1 — passive citation extractor.
 *
 * <p>Given a customer-visible bot response and the set of articles the
 * runtime retrieved or resolved this turn, return the subset that the bot
 * actually <em>cited</em>. "Cited" is detected by three deterministic
 * patterns, in order of confidence:
 *
 * <ol>
 *   <li>Salesforce Knowledge {@code source_id} mentions (e.g.
 *       {@code ka4P2000000021pIAA}).</li>
 *   <li>Canonical URL mentions matching a Gumtree help URL or any URL the
 *       runtime supplied as a {@code canonical_url} for one of the
 *       retrieved / resolved articles.</li>
 *   <li>Title fuzzy match against any retrieved / resolved article whose
 *       title is non-empty and reasonably specific.</li>
 * </ol>
 *
 * <p>Sprint 14 explicitly does NOT use the result of this extractor to
 * reject, rewrite, or loop the response. The output is observability
 * material consumed by:
 *
 * <ul>
 *   <li>{@link SourceEvidenceLineage#citedSourceIds()} (turn-level
 *       evidence breakdown).</li>
 *   <li>The §L2 grounding diagnostics (e.g.
 *       {@code resolved_but_uncited}, {@code citation_present}).</li>
 * </ul>
 *
 * <p>The class is stateless and side-effect-free so it can be invoked
 * cheaply from {@code ControlKernel.recordRunResult} and unit-tested
 * without Spring wiring.
 */
public final class CitationExtractor {

    /**
     * Salesforce Knowledge IDs always start with {@code ka} and are 15 or
     * 18-character alphanumerics. The pattern is intentionally tight to
     * avoid false positives on unrelated alphanumerics that happen to
     * occur in customer-facing copy.
     */
    private static final Pattern SOURCE_ID_PATTERN =
            Pattern.compile("\\bka[A-Za-z0-9]{13}([A-Za-z0-9]{3})?\\b");

    /**
     * Greedy URL pattern used both to detect direct URL mentions in the
     * bot reply and to test whether the bot quoted any of the canonical
     * URLs the runtime supplied this turn. Trims trailing punctuation that
     * commonly attaches to URLs in prose ({@code . , ; ) ] '"}).
     */
    private static final Pattern URL_PATTERN =
            Pattern.compile("https?://\\S+");

    private CitationExtractor() {}

    /**
     * Extract the cited subset of {@code candidates} present in {@code text}.
     *
     * @param text       customer-visible bot response (may be null / blank).
     * @param candidates known article candidates (retrieved + resolved
     *                   union, in any order). May be empty.
     * @return extracted citations. Never null.
     */
    public static Citations extract(String text, Collection<ArticleCandidate> candidates) {
        if (text == null || text.isBlank()) {
            return Citations.empty();
        }
        if (candidates == null) {
            candidates = List.of();
        }

        Set<String> citedSourceIds = new LinkedHashSet<>();
        Set<String> citedCanonicalUrls = new LinkedHashSet<>();

        // Pattern 1 — direct source_id mentions.
        Matcher idMatcher = SOURCE_ID_PATTERN.matcher(text);
        while (idMatcher.find()) {
            citedSourceIds.add(idMatcher.group());
        }

        // Pattern 2 — URL mentions. Match against the supplied canonical
        // URLs first (so we can map a URL citation back to a source_id);
        // otherwise record the raw URL as a stand-alone citation.
        List<String> textUrls = new ArrayList<>();
        Matcher urlMatcher = URL_PATTERN.matcher(text);
        while (urlMatcher.find()) {
            textUrls.add(stripTrailingPunctuation(urlMatcher.group()));
        }
        for (String url : textUrls) {
            boolean matched = false;
            for (ArticleCandidate candidate : candidates) {
                if (candidate.canonicalUrl() != null
                        && !candidate.canonicalUrl().isBlank()
                        && candidate.canonicalUrl().equalsIgnoreCase(url)) {
                    citedSourceIds.add(candidate.sourceId());
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                citedCanonicalUrls.add(url);
            }
        }

        // Pattern 3 — title fuzzy match. Only fire when the title is at
        // least three words long (avoids false positives like "Login" or
        // "Help") and appears as a contiguous substring (case-insensitive).
        // The §L1 task description explicitly notes this match is
        // "if safe and deterministic"; this is intentionally conservative.
        String lowered = text.toLowerCase(Locale.ROOT);
        for (ArticleCandidate candidate : candidates) {
            if (citedSourceIds.contains(candidate.sourceId())) {
                continue;
            }
            String title = candidate.title();
            if (title == null) continue;
            String trimmed = title.trim();
            if (trimmed.split("\\s+").length < 3) continue;
            if (lowered.contains(trimmed.toLowerCase(Locale.ROOT))) {
                citedSourceIds.add(candidate.sourceId());
            }
        }

        return new Citations(
                List.copyOf(citedSourceIds),
                List.copyOf(citedCanonicalUrls));
    }

    private static String stripTrailingPunctuation(String url) {
        int end = url.length();
        while (end > 0) {
            char ch = url.charAt(end - 1);
            if (ch == '.' || ch == ',' || ch == ';' || ch == ')' || ch == ']'
                    || ch == '"' || ch == '\'') {
                end--;
            } else {
                break;
            }
        }
        return url.substring(0, end);
    }

    /**
     * Article candidate hint: the runtime tells the extractor which
     * articles were retrieved / resolved this turn so a URL or title
     * match can be mapped back to a {@code source_id}.
     */
    public record ArticleCandidate(String sourceId, String title, String canonicalUrl) {}

    /**
     * Output of {@link #extract}. {@code citedSourceIds} is the list of
     * Salesforce Knowledge IDs detected (either directly or via URL /
     * title match); {@code citedCanonicalUrls} is the list of free-form
     * URLs cited in the message that did NOT correspond to a known
     * candidate (typically a third-party domain such as gov.uk).
     */
    public record Citations(List<String> citedSourceIds, List<String> citedCanonicalUrls) {

        public static Citations empty() {
            return new Citations(List.of(), List.of());
        }

        public boolean isEmpty() {
            return citedSourceIds.isEmpty() && citedCanonicalUrls.isEmpty();
        }
    }
}
