package io.github.krushnatkhawale.blogify.core;

import io.github.krushnatkhawale.blogify.core.model.BlogPostModel;
import io.github.krushnatkhawale.blogify.core.parser.MarkdownBlogParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class MarkdownBlogParserTest {

    private final MarkdownBlogParser parser = new MarkdownBlogParser();

    @Test
    void shouldParseMarkdownPostWithStoryFirstMetadata(@TempDir Path tempDir) throws IOException {
        Path postBundleDir = tempDir.resolve("blogs/resilient-orders");
        Files.createDirectories(postBundleDir);
        Path indexMd = postBundleDir.resolve("index.md");

        String content = """
                ---
                title: "Resilient Order Processing"
                author: "Core Team"
                date: "2026-11-21"
                description: "Building zero-loss order queues."
                tags: [resilience, queue]
                version: "v2.1.0"
                commit: "abc1234"
                repoUrl: "https://github.com/example/blogify"
                status: "active"
                problem: |
                  High latency causes client retries and duplicate records.
                insight: |
                  Use distributed hash idempotency tokens in Redis.
                diagram: |
                  sequenceDiagram
                    Client->>Server: Request
                takeaways:
                  - "Never trust client timestamps"
                  - "Always return cached 200 on retry"
                ---
                
                ## Overview
                This is the article body.
                """;
        Files.writeString(indexMd, content);

        BlogPostModel post = parser.parse(indexMd.toFile());

        assertNotNull(post);
        assertEquals("Resilient Order Processing", post.getTitle());
        assertEquals("resilient-orders", post.getSlug());
        assertEquals("Core Team", post.getAuthor());
        assertEquals("2026-11-21", post.getDate());
        assertEquals("v2.1.0", post.getVersion());
        assertEquals("abc1234", post.getCommitSha());
        assertEquals("https://github.com/example/blogify", post.getRepoUrl());
        assertFalse(post.isRetired());
        assertEquals("High latency causes client retries and duplicate records.", post.getProblem());
        assertEquals("Use distributed hash idempotency tokens in Redis.", post.getInsight());
        assertEquals("sequenceDiagram\nClient->>Server: Request", post.getDiagram());
        assertEquals(2, post.getTakeaways().size());
        assertEquals("Never trust client timestamps", post.getTakeaways().get(0));
        assertTrue(post.getRawBody().contains("## Overview"));
    }

    @Test
    void shouldParseRetiredStatusAndSupersededBy(@TempDir Path tempDir) throws IOException {
        Path indexMd = tempDir.resolve("index.md");
        String content = """
                ---
                title: "Old Architecture"
                status: "retired"
                supersededBy: "new-architecture"
                supersededMessage: "Moved to event sourcing"
                ---
                Deprecated content.
                """;
        Files.writeString(indexMd, content);

        BlogPostModel post = parser.parse(indexMd.toFile());

        assertNotNull(post);
        assertTrue(post.isRetired());
        assertEquals("new-architecture", post.getSupersededBy());
        assertEquals("Moved to event sourcing", post.getSupersededMessage());
    }
}
