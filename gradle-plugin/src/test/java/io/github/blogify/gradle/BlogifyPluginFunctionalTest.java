package io.github.blogify.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogifyPluginFunctionalTest {

    @TempDir
    Path testProjectDir;

    @Test
    void testGenerateBlogTaskExecution() throws IOException {
        // settings.gradle
        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'test-app'");

        // build.gradle
        String buildScript = """
                plugins {
                    id 'java'
                    id 'io.github.blogify'
                }

                blogify {
                    siteTitle = 'Test Tech Blog'
                }
                """;
        Files.writeString(testProjectDir.resolve("build.gradle"), buildScript);

        // Java source with @BlogPost
        Path srcDir = testProjectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        String sampleSource = """
                package com.example;

                import io.github.blogify.core.annotation.BlogPost;
                import io.github.blogify.core.annotation.BlogSection;
                import io.github.blogify.core.annotation.BlogCodeBlock;

                /**
                 * Microservices routing architecture description.
                 */
                @BlogPost(
                    title = "Microservices Routing Architecture",
                    slug = "microservices-routing-architecture",
                    author = "System Architect",
                    date = "2026-11-21",
                    tags = {"architecture", "routing"}
                )
                public class GatewayService {

                    @BlogSection(heading = "Routing Logic")
                    @BlogCodeBlock(description = "Route evaluation")
                    public String routeRequest(String path) {
                        return "upstream: " + path;
                    }
                }
                """;
        Files.writeString(srcDir.resolve("GatewayService.java"), sampleSource);

        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments("generateBlog", "--stacktrace")
                .build();

        Path outputDir = testProjectDir.resolve("build/blog");
        Path generatedPost = outputDir.resolve("microservices-routing-architecture.md");
        Path generatedIndex = outputDir.resolve("index.md");

        assertTrue(Files.exists(generatedPost), "Generated post Markdown file should exist: " + generatedPost);
        assertTrue(Files.exists(generatedIndex), "Generated index.md should exist: " + generatedIndex);

        String postContent = Files.readString(generatedPost);
        assertTrue(postContent.contains("title: \"Microservices Routing Architecture\""));
        assertTrue(postContent.contains("## Routing Logic"));
        assertTrue(postContent.contains("```java"));

        String indexContent = Files.readString(generatedIndex);
        assertTrue(indexContent.contains("Test Tech Blog"));
        assertTrue(indexContent.contains("microservices-routing-architecture.md"));
    }
}
