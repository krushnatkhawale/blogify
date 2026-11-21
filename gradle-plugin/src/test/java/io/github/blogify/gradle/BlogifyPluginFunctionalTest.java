package io.github.blogify.gradle;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogifyPluginFunctionalTest {

    @TempDir
    Path testProjectDir;

    @Test
    void testGenerateBlogTaskExecutionDefaultGitHubPages() throws IOException {
        // settings.gradle
        Files.writeString(testProjectDir.resolve("settings.gradle"), "rootProject.name = 'test-app'");

        // build.gradle
        String buildScript = """
                plugins {
                    id 'java'
                    id 'io.github.krushnatkhawale.blogify'
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
        Path generatedConfig = outputDir.resolve("_config.yml");
        Path generatedIndex = outputDir.resolve("index.md");
        Path generatedPost = outputDir.resolve("_posts/2026-11-21-microservices-routing-architecture.md");
        Path previewIndex = outputDir.resolve("preview/index.html");

        assertTrue(Files.exists(generatedConfig), "_config.yml should exist: " + generatedConfig);
        assertTrue(Files.exists(generatedIndex), "index.md should exist: " + generatedIndex);
        assertTrue(Files.exists(generatedPost), "_posts/ post Markdown file should exist: " + generatedPost);
        assertTrue(Files.exists(previewIndex), "preview/index.html should exist: " + previewIndex);

        String postContent = Files.readString(generatedPost);
        assertTrue(postContent.contains("layout: post"));
        assertTrue(postContent.contains("title: \"Microservices Routing Architecture\""));
        assertTrue(postContent.contains("## Routing Logic"));
        assertTrue(postContent.contains("```java"));

        String configContent = Files.readString(generatedConfig);
        assertTrue(configContent.contains("theme: minima"));
        assertTrue(configContent.contains("Test Tech Blog"));

        // Also test previewBlog with --no-wait option
        BuildResult previewResult = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withPluginClasspath()
                .withArguments("previewBlog", "--no-wait", "--port", "9191")
                .build();

        assertTrue(previewResult.getOutput().contains("Blogify Local Preview Server"));
        assertTrue(previewResult.getOutput().contains("http://localhost:9191/"));
    }

    @Test
    void testHtmlFormatGeneration() throws IOException {
        Path htmlProjectDir = testProjectDir.resolve("html-app");
        Files.createDirectories(htmlProjectDir);

        Files.writeString(htmlProjectDir.resolve("settings.gradle"), "rootProject.name = 'html-app'");

        String buildScript = """
                plugins {
                    id 'java'
                    id 'io.github.krushnatkhawale.blogify'
                }

                blogify {
                    siteTitle = 'HTML Engineering Blog'
                    format = 'html'
                }
                """;
        Files.writeString(htmlProjectDir.resolve("build.gradle"), buildScript);

        Path srcDir = htmlProjectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);

        String sampleSource = """
                package com.example;

                import io.github.blogify.core.annotation.BlogPost;

                @BlogPost(
                    title = "Static HTML Article",
                    slug = "static-html-article",
                    author = "Web Team"
                )
                public class ArticleService {}
                """;
        Files.writeString(srcDir.resolve("ArticleService.java"), sampleSource);

        BuildResult result = GradleRunner.create()
                .withProjectDir(htmlProjectDir.toFile())
                .withPluginClasspath()
                .withArguments("generateBlog", "--stacktrace")
                .build();

        Path outputDir = htmlProjectDir.resolve("build/blog");
        Path noJekyll = outputDir.resolve(".nojekyll");
        Path cssFile = outputDir.resolve("style.css");
        Path indexFile = outputDir.resolve("index.html");
        Path postFile = outputDir.resolve("static-html-article.html");

        assertTrue(Files.exists(noJekyll), ".nojekyll should exist");
        assertTrue(Files.exists(cssFile), "style.css should exist");
        assertTrue(Files.exists(indexFile), "index.html should exist");
        assertTrue(Files.exists(postFile), "static-html-article.html should exist");
    }
}
