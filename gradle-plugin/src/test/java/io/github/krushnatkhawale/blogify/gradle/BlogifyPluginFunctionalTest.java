package io.github.krushnatkhawale.blogify.gradle;

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

                import io.github.krushnatkhawale.blogify.core.annotation.BlogPost;
                import io.github.krushnatkhawale.blogify.core.annotation.BlogSection;
                import io.github.krushnatkhawale.blogify.core.annotation.BlogCodeBlock;

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

                import io.github.krushnatkhawale.blogify.core.annotation.BlogPost;

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

    @Test
    void testGenerateBlogWithConfiguredBlogHome(@TempDir Path blogHomeProjectDir) throws IOException {
        Files.writeString(blogHomeProjectDir.resolve("settings.gradle"), "rootProject.name = 'bloghome-app'");

        String buildScript = """
                plugins {
                    id 'java'
                    id 'io.github.krushnatkhawale.blogify'
                }

                blogify {
                    siteTitle = 'Configured Blog'
                    blogHome = 'https://krushnatkhawale.github.io/#/blogs'
                }
                """;
        Files.writeString(blogHomeProjectDir.resolve("build.gradle"), buildScript);

        Path blogsDir = blogHomeProjectDir.resolve("blogs/my-post");
        Files.createDirectories(blogsDir);
        String md = """
                ---
                title: "My Configured Post"
                slug: "my-post"
                home: true
                ---
                # Content
                """;
        Files.writeString(blogsDir.resolve("index.md"), md);

        BuildResult result = GradleRunner.create()
                .withProjectDir(blogHomeProjectDir.toFile())
                .withPluginClasspath()
                .withArguments("generateBlog", "--stacktrace")
                .build();

        Path previewIndex = blogHomeProjectDir.resolve("build/blog/preview/index.html");
        assertTrue(Files.exists(previewIndex), "preview/index.html should exist");

        String previewHtml = Files.readString(previewIndex);
        assertTrue(previewHtml.contains("href=\"https://krushnatkhawale.github.io/#/blogs\""),
                "preview HTML should contain configured blogHome URL");
        assertTrue(previewHtml.contains("← Back to Blogs"), "should have back to blogs link");
    }

    @Test
    void testMultiModuleBlogGeneration() throws IOException {
        Path multiProjectDir = testProjectDir.resolve("multi-app");
        Files.createDirectories(multiProjectDir);

        Files.writeString(multiProjectDir.resolve("settings.gradle"),
                "rootProject.name = 'multi-app'\ninclude 'adapter', 'springai-batch-metadata-client'");

        Files.createDirectories(multiProjectDir.resolve("adapter"));
        Files.createDirectories(multiProjectDir.resolve("springai-batch-metadata-client"));

        String buildScript = """
                plugins {
                    id 'io.github.krushnatkhawale.blogify' apply false
                }

                subprojects {
                    apply plugin: 'java'
                    apply plugin: 'io.github.krushnatkhawale.blogify'
                    version = '1.0.0'
                }
                """;
        Files.writeString(multiProjectDir.resolve("build.gradle"), buildScript);

        // Root blogs/adapter and blogs/springai-batch-metadata-client
        Path adapterBlogDir = multiProjectDir.resolve("blogs/adapter");
        Files.createDirectories(adapterBlogDir);
        String adapterMd = """
                ---
                title: "Adapter Architecture Guide"
                siteTitle: "Spring Batch Integration Adapter"
                ---
                Adapter guide content.
                """;
        Files.writeString(adapterBlogDir.resolve("index.md"), adapterMd);

        Path clientBlogDir = multiProjectDir.resolve("blogs/springai-batch-metadata-client");
        Files.createDirectories(clientBlogDir);
        String clientMd = """
                ---
                title: "Client Architecture Guide"
                siteTitle: "SpringAI Batch Metadata Client"
                ---
                Client guide content.
                """;
        Files.writeString(clientBlogDir.resolve("index.md"), clientMd);

        // Run both submodules' generateBlog tasks
        BuildResult result = GradleRunner.create()
                .withProjectDir(multiProjectDir.toFile())
                .withPluginClasspath()
                .withArguments(":adapter:generateBlog", ":springai-batch-metadata-client:generateBlog", "--stacktrace")
                .build();

        Path adapterOut = multiProjectDir.resolve("adapter/build/blog/spring-batch-integration-adapter/1.0.0/index.html");
        Path clientOut = multiProjectDir.resolve("springai-batch-metadata-client/build/blog/springai-batch-metadata-client/1.0.0/index.html");

        assertTrue(Files.exists(adapterOut), "Adapter versioned output should exist: " + adapterOut);
        assertTrue(Files.exists(clientOut), "Client versioned output should exist: " + clientOut);

        String adapterHtml = Files.readString(adapterOut);
        assertTrue(adapterHtml.contains("Adapter Architecture Guide"));

        String clientHtml = Files.readString(clientOut);
        assertTrue(clientHtml.contains("Client Architecture Guide"));
    }
}
