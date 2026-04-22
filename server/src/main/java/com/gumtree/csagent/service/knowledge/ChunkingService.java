package com.gumtree.csagent.service.knowledge;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits article text into overlapping chunks suitable for embedding.
 * Target chunk size: 256-512 tokens (~4 chars per token).
 * Overlap: ~15% between consecutive chunks.
 */
@Slf4j
@Service
public class ChunkingService {

    /** Target chunk size in characters (roughly 384 tokens * 4 chars/token). */
    private static final int TARGET_CHUNK_CHARS = 1536;

    /** Maximum chunk size in characters (512 tokens * 4 chars/token). */
    private static final int MAX_CHUNK_CHARS = 2048;

    /** Minimum chunk size in characters (256 tokens * 4 chars/token). */
    private static final int MIN_CHUNK_CHARS = 256;

    /** Overlap between consecutive chunks as a fraction (15%). */
    private static final double OVERLAP_FRACTION = 0.15;

    /**
     * Clean HTML from text using Jsoup, returning plain text.
     */
    public String cleanHtml(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        // Parse HTML and extract text; Jsoup handles entity decoding
        String cleaned = Jsoup.clean(html, Safelist.none());
        // Jsoup.clean may leave &nbsp; etc; parse again for text extraction
        String text = Jsoup.parse(cleaned).text();
        // Normalize whitespace
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Chunk the given article text using sliding window with overlap.
     *
     * @param articleTitle    Title of the article (used as default section heading)
     * @param articleText     Plain text content (HTML should be cleaned before calling)
     * @return list of chunk records
     */
    public List<ChunkRecord> chunk(String articleTitle, String articleText) {
        List<ChunkRecord> chunks = new ArrayList<>();

        if (articleText == null || articleText.isBlank()) {
            return chunks;
        }

        String text = articleText.trim();

        // If text fits in a single chunk, return it directly
        if (text.length() <= MAX_CHUNK_CHARS) {
            chunks.add(new ChunkRecord(text, 0, extractSectionHeading(text, articleTitle)));
            return chunks;
        }

        int overlapChars = (int) (TARGET_CHUNK_CHARS * OVERLAP_FRACTION);
        int stepSize = TARGET_CHUNK_CHARS - overlapChars;
        int index = 0;
        int pos = 0;

        while (pos < text.length()) {
            int end = Math.min(pos + TARGET_CHUNK_CHARS, text.length());

            // Try to break at a sentence boundary or paragraph break
            if (end < text.length()) {
                end = findBreakPoint(text, pos, end);
            }

            String chunkText = text.substring(pos, end).trim();
            if (chunkText.length() >= MIN_CHUNK_CHARS || pos + stepSize >= text.length()) {
                String heading = extractSectionHeading(chunkText, articleTitle);
                chunks.add(new ChunkRecord(chunkText, index, heading));
                index++;
            }

            // Advance by step size, but at least 1 character to avoid infinite loop
            int nextPos = pos + Math.max(end - pos - overlapChars, 1);
            if (nextPos <= pos) {
                nextPos = pos + 1;
            }
            pos = nextPos;
        }

        log.debug("Chunked article '{}': {} chars -> {} chunks", articleTitle, text.length(), chunks.size());
        return chunks;
    }

    /**
     * Find a good break point near the target end position.
     * Prefers paragraph breaks, then sentence ends, then word boundaries.
     */
    private int findBreakPoint(String text, int start, int targetEnd) {
        int searchFrom = Math.max(targetEnd - 200, start);

        // Look for paragraph break (double newline)
        int paragraphBreak = text.lastIndexOf("\n\n", targetEnd);
        if (paragraphBreak > searchFrom) {
            return paragraphBreak + 2;
        }

        // Look for sentence end
        for (int i = targetEnd; i > searchFrom; i--) {
            char c = text.charAt(i - 1);
            if ((c == '.' || c == '!' || c == '?') && i < text.length() && Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }

        // Look for word boundary
        int spacePos = text.lastIndexOf(' ', targetEnd);
        if (spacePos > searchFrom) {
            return spacePos + 1;
        }

        return targetEnd;
    }

    /**
     * Extract a section heading from the first line of a chunk, or use article title.
     */
    private String extractSectionHeading(String chunkText, String articleTitle) {
        if (chunkText == null || chunkText.isBlank()) {
            return articleTitle;
        }

        // Check if text starts with a short heading-like line
        int firstNewline = chunkText.indexOf('\n');
        if (firstNewline > 0 && firstNewline < 100) {
            String firstLine = chunkText.substring(0, firstNewline).trim();
            // If it looks like a heading (short, no period at end)
            if (!firstLine.endsWith(".") && firstLine.length() < 80) {
                return firstLine;
            }
        }

        return articleTitle;
    }

    /**
     * Estimate token count for text (~4 chars per token).
     */
    public int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * Record representing a single chunk of text.
     */
    public record ChunkRecord(String text, int index, String sectionHeading) {}
}
