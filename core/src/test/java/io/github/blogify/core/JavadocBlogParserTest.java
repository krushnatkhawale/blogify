package io.github.blogify.core;

import io.github.blogify.core.generator.MarkdownGenerator;
import io.github.blogify.core.model.BlogPostModel;
import io.github.blogify.core.parser.JavaSourceBlogParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavadocBlogParserTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseBlogPostFromPureJavadocWithoutAnyAnnotations() throws IOException {
        String pureJavaCode = """
                package com.example.service;

                /**
                 * High-availability cluster management service.
                 * Coordinates node discovery, heartbeat monitoring, and failover leadership.
                 *
                 * @author Cluster Core Team
                 * @since 2026-11-21
                 * @blog.title High Availability Cluster Architecture
                 * @blog.slug ha-cluster-architecture
                 * @blog.tags distributed-systems, clustering, high-availability
                 * @blog.order 1
                 */
                public class ClusterService {

                    /**
                     * Dispatches health check pings across active cluster nodes.
                     *
                     * @blog.section Heartbeat and Health Probing
                     * @blog.codeblock Cluster node ping cycle
                     * @blog.order 1
                     */
                    public boolean checkHeartbeat(String nodeId) {
                        return nodeId != null && !nodeId.isBlank();
                    }

                    /**
                     * Internal helper method not meant for the public blog.
                     *
                     * @blog.ignore
                     */
                    public void internalSync() {
                        // ignore
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("ClusterService.java");
        Files.writeString(sourceFile, pureJavaCode);

        JavaSourceBlogParser parser = new JavaSourceBlogParser();
        List<BlogPostModel> posts = parser.parse(sourceFile);

        assertEquals(1, posts.size(), "Should find 1 blog post from pure Javadoc");
        BlogPostModel post = posts.get(0);

        assertEquals("High Availability Cluster Architecture", post.getTitle());
        assertEquals("ha-cluster-architecture", post.getSlug());
        assertEquals("Cluster Core Team", post.getAuthor());
        assertEquals("2026-11-21", post.getDate());
        assertTrue(post.getTags().contains("distributed-systems"));
        assertTrue(post.getTags().contains("clustering"));
        assertTrue(post.getTags().contains("high-availability"));
        assertTrue(post.getIntroContent().contains("High-availability cluster management service"));

        // Only 1 section (internalSync was ignored)
        assertEquals(1, post.getSections().size());
        assertEquals("Heartbeat and Health Probing", post.getSections().get(0).getHeading());
        assertFalse(post.getSections().get(0).getCodeBlocks().isEmpty());

        // Verify clean code block extraction without any annotations
        String code = post.getSections().get(0).getCodeBlocks().get(0).getCode();
        assertTrue(code.contains("public boolean checkHeartbeat(String nodeId)"));
        assertFalse(code.contains("@Blog"));

        MarkdownGenerator generator = new MarkdownGenerator();
        String markdown = generator.generate(post);
        assertNotNull(markdown);
        assertTrue(markdown.contains("title: \"High Availability Cluster Architecture\""));
        assertTrue(markdown.contains("## Heartbeat and Health Probing"));
        assertTrue(markdown.contains("```java"));
    }

    @Test
    void shouldAutoInferMetadataFromStandardJavadocTags() throws IOException {
        String javaCode = """
                package com.example.service;

                /**
                 * Automatic rate limiting engine.
                 *
                 * @blog
                 * @author Infra Engineering
                 * @since 2026-11-21
                 */
                public class RateLimiterService {

                    /**
                     * Verifies token bucket capacity before permitting request.
                     */
                    public boolean allowRequest(String clientId) {
                        return true;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("RateLimiterService.java");
        Files.writeString(sourceFile, javaCode);

        JavaSourceBlogParser parser = new JavaSourceBlogParser();
        List<BlogPostModel> posts = parser.parse(sourceFile);

        assertEquals(1, posts.size());
        BlogPostModel post = posts.get(0);

        // Inferred title from class name: RateLimiterService -> Rate Limiter Service
        assertEquals("Rate Limiter Service", post.getTitle());
        assertEquals("rate-limiter-service", post.getSlug());
        assertEquals("Infra Engineering", post.getAuthor());
        assertEquals("2026-11-21", post.getDate());

        // Inferred method heading: allowRequest -> Allow Request
        assertEquals(1, post.getSections().size());
        assertEquals("Allow Request", post.getSections().get(0).getHeading());
    }
}
