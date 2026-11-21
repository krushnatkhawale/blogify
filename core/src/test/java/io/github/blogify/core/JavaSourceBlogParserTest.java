package io.github.blogify.core;

import io.github.blogify.core.generator.MarkdownGenerator;
import io.github.blogify.core.model.BlogPostModel;
import io.github.blogify.core.parser.JavaSourceBlogParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceBlogParserTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldParseBlogPostFromJavaSource() throws IOException {
        String javaCode = """
                package com.example.service;

                import io.github.blogify.core.annotation.BlogPost;
                import io.github.blogify.core.annotation.BlogSection;
                import io.github.blogify.core.annotation.BlogCodeBlock;
                import io.github.blogify.core.annotation.BlogTag;

                /**
                 * A high throughput payment service handling multi-currency transactions.
                 */
                @BlogPost(
                    title = "Building a Resilient Payment Gateway",
                    slug = "building-resilient-payment-gateway",
                    author = "Dev Team",
                    date = "2026-11-21",
                    tags = {"payments", "architecture"}
                )
                @BlogTag("microservices")
                public class PaymentService {

                    /**
                     * Process transaction asynchronously with retry mechanisms.
                     */
                    @BlogSection(heading = "Asynchronous Processing", order = 1)
                    @BlogCodeBlock(description = "Payment processing logic")
                    public String processPayment(String orderId, double amount) {
                        return "PROCESSED: " + orderId;
                    }
                }
                """;

        Path sourceFile = tempDir.resolve("PaymentService.java");
        Files.writeString(sourceFile, javaCode);

        JavaSourceBlogParser parser = new JavaSourceBlogParser();
        List<BlogPostModel> posts = parser.parse(sourceFile);

        assertEquals(1, posts.size());
        BlogPostModel post = posts.get(0);

        assertEquals("Building a Resilient Payment Gateway", post.getTitle());
        assertEquals("building-resilient-payment-gateway", post.getSlug());
        assertEquals("Dev Team", post.getAuthor());
        assertEquals("2026-11-21", post.getDate());
        assertTrue(post.getTags().contains("payments"));
        assertTrue(post.getTags().contains("architecture"));
        assertTrue(post.getTags().contains("microservices"));
        assertTrue(post.getIntroContent().contains("A high throughput payment service"));

        assertEquals(1, post.getSections().size());
        assertEquals("Asynchronous Processing", post.getSections().get(0).getHeading());
        assertFalse(post.getSections().get(0).getCodeBlocks().isEmpty());

        MarkdownGenerator generator = new MarkdownGenerator();
        String markdown = generator.generate(post);
        assertNotNull(markdown);
        assertTrue(markdown.contains("title: \"Building a Resilient Payment Gateway\""));
        assertTrue(markdown.contains("# Building a Resilient Payment Gateway"));
        assertTrue(markdown.contains("```java"));
        assertTrue(markdown.contains("public String processPayment"));
    }
}
