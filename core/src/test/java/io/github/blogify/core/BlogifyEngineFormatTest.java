package io.github.blogify.core;

import io.github.blogify.core.generator.BlogFormat;
import io.github.blogify.core.generator.BlogifyEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogifyEngineFormatTest {

    @TempDir
    Path tempDir;

    @Test
    void testBlogFormatEnum() {
        assertEquals(BlogFormat.GITHUB_PAGES, BlogFormat.fromString("github-pages"));
        assertEquals(BlogFormat.GITHUB_PAGES, BlogFormat.fromString("gh-pages"));
        assertEquals(BlogFormat.GITHUB_PAGES, BlogFormat.fromString("jekyll"));
        assertEquals(BlogFormat.HTML, BlogFormat.fromString("html"));
        assertEquals(BlogFormat.MARKDOWN, BlogFormat.fromString("markdown"));
        assertEquals(BlogFormat.GITHUB_PAGES, BlogFormat.fromString(null));
        assertEquals(BlogFormat.GITHUB_PAGES, BlogFormat.fromString("unknown"));
    }

    @Test
    void testGitHubPagesGenerationDefault() throws IOException {
        Path srcDir = tempDir.resolve("src");
        Files.createDirectories(srcDir);

        String sampleClass = """
                package com.example;
                import io.github.blogify.core.annotation.BlogPost;
                import io.github.blogify.core.annotation.BlogSection;
                import io.github.blogify.core.annotation.BlogCodeBlock;

                /**
                 * Cache architecture documentation.
                 */
                @BlogPost(
                    title = "Distributed Cache Architecture",
                    slug = "distributed-cache-architecture",
                    author = "Infra Team",
                    date = "2026-11-21",
                    tags = {"cache", "redis"}
                )
                public class CacheService {
                    @BlogSection(heading = "Eviction Policy")
                    @BlogCodeBlock(description = "LRU eviction check")
                    public boolean evictKey(String key) {
                        return true;
                    }
                }
                """;
        Files.writeString(srcDir.resolve("CacheService.java"), sampleClass);

        Path outDir = tempDir.resolve("out_gh");
        BlogifyEngine engine = new BlogifyEngine();
        List<Path> files = engine.generate(Collections.singletonList(srcDir.toFile()), outDir.toFile(), "Tech Blog", true, "github-pages");

        // Verify GitHub Pages Jekyll files
        Path configFile = outDir.resolve("_config.yml");
        Path indexFile = outDir.resolve("index.md");
        Path postsDir = outDir.resolve("_posts");
        Path postFile = postsDir.resolve("2026-11-21-distributed-cache-architecture.md");

        assertTrue(Files.exists(configFile), "_config.yml should exist");
        assertTrue(Files.exists(indexFile), "index.md should exist");
        assertTrue(Files.exists(postFile), "_posts/2026-11-21-distributed-cache-architecture.md should exist");

        String configContent = Files.readString(configFile);
        assertTrue(configContent.contains("theme: minima"));
        assertTrue(configContent.contains("title: \"Tech Blog\""));

        String postContent = Files.readString(postFile);
        assertTrue(postContent.contains("layout: post"));
        assertTrue(postContent.contains("title: \"Distributed Cache Architecture\""));

        // Verify preview was also generated
        Path previewIndex = outDir.resolve("preview/index.html");
        assertTrue(Files.exists(previewIndex), "Local HTML preview index should exist");
    }

    @Test
    void testHtmlGeneration() throws IOException {
        Path srcDir = tempDir.resolve("src_html");
        Files.createDirectories(srcDir);

        String sampleClass = """
                package com.example;
                import io.github.blogify.core.annotation.BlogPost;

                @BlogPost(
                    title = "HTML Blog Post",
                    slug = "html-blog-post",
                    author = "Frontend Team"
                )
                public class HtmlService {}
                """;
        Files.writeString(srcDir.resolve("HtmlService.java"), sampleClass);

        Path outDir = tempDir.resolve("out_html");
        BlogifyEngine engine = new BlogifyEngine();
        List<Path> files = engine.generate(Collections.singletonList(srcDir.toFile()), outDir.toFile(), "HTML Blog", true, "html");

        Path noJekyll = outDir.resolve(".nojekyll");
        Path cssFile = outDir.resolve("style.css");
        Path indexFile = outDir.resolve("index.html");
        Path postFile = outDir.resolve("html-blog-post.html");

        assertTrue(Files.exists(noJekyll), ".nojekyll should exist");
        assertTrue(Files.exists(cssFile), "style.css should exist");
        assertTrue(Files.exists(indexFile), "index.html should exist");
        assertTrue(Files.exists(postFile), "html-blog-post.html should exist");

        String indexHtml = Files.readString(indexFile);
        assertTrue(indexHtml.contains("HTML Blog Post"));
    }

    @Test
    void testHtmlGenerationWithHomePost() throws IOException {
        Path blogDir = tempDir.resolve("blogs/blogify");
        Files.createDirectories(blogDir);

        String postMd = """
                ---
                title: "Blogify Engine"
                slug: "blogify"
                home: true
                author: "Core Team"
                problem: "Documentation drift"
                insight: "Compile-time generation"
                takeaways:
                  - "Zero runtime dependencies"
                ---
                
                # Blogify Overview
                The static blog engine.
                """;
        Files.writeString(blogDir.resolve("index.md"), postMd);

        Path otherBlogDir = tempDir.resolve("blogs/other");
        Files.createDirectories(otherBlogDir);
        String otherMd = """
                ---
                title: "Other Guide"
                slug: "other-guide"
                ---
                Other content
                """;
        Files.writeString(otherBlogDir.resolve("index.md"), otherMd);

        Path outDir = tempDir.resolve("out_home");
        BlogifyEngine engine = new BlogifyEngine();
        engine.generate(Collections.singletonList(tempDir.resolve("blogs").toFile()), outDir.toFile(), "Blogify", true, "html");

        Path indexFile = outDir.resolve("index.html");
        Path slugFile = outDir.resolve("blogify.html");
        Path articlesFile = outDir.resolve("articles.html");
        Path otherFile = outDir.resolve("other-guide.html");

        assertTrue(Files.exists(indexFile), "index.html should exist");
        assertTrue(Files.exists(slugFile), "blogify.html should exist");
        assertTrue(Files.exists(articlesFile), "articles.html should exist");
        assertTrue(Files.exists(otherFile), "other-guide.html should exist");

        String indexHtml = Files.readString(indexFile);
        // index.html should contain the home article content directly
        assertTrue(indexHtml.contains("Blogify Engine"), "index.html should contain title");
        assertTrue(indexHtml.contains("Documentation drift"), "index.html should contain problem callout");
        assertTrue(indexHtml.contains("Compile-time generation"), "index.html should contain insight callout");
        assertTrue(indexHtml.contains("More Architecture Guides"), "index.html should have more guides section");

        String articlesHtml = Files.readString(articlesFile);
        assertTrue(articlesHtml.contains("Other Guide"), "articles.html should list other guide");
        assertTrue(articlesHtml.contains("Blogify Articles"), "articles.html should have articles heading");
    }
}
