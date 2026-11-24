package io.github.krushnatkhawale.blogify.core;

import io.github.krushnatkhawale.blogify.core.generator.BlogFormat;
import io.github.krushnatkhawale.blogify.core.generator.BlogifyEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                import io.github.krushnatkhawale.blogify.core.annotation.BlogPost;
                import io.github.krushnatkhawale.blogify.core.annotation.BlogSection;
                import io.github.krushnatkhawale.blogify.core.annotation.BlogCodeBlock;

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
                import io.github.krushnatkhawale.blogify.core.annotation.BlogPost;

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

    @Test
    void testHtmlGenerationWithBlogHomeAndPortfolioJson() throws IOException {
        Path blogDir = tempDir.resolve("blogs/blogify");
        Files.createDirectories(blogDir);

        String postMd = """
                ---
                title: "Blogify Engine"
                slug: "blogify"
                home: true
                category: "Developer Tools"
                date: "2026-11-20"
                description: "Automated blogging"
                tags: [Java, Gradle]
                repoUrl: "https://github.com/krushnatkhawale/blogify"
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
                category: "Architecture"
                tags: [Microservices]
                repoUrl: "https://github.com/krushnatkhawale/blogify"
                ---
                Other content
                """;
        Files.writeString(otherBlogDir.resolve("index.md"), otherMd);

        Path outDir = tempDir.resolve("out_bloghome");
        BlogifyEngine engine = new BlogifyEngine();
        String blogHome = "http://localhost:3000/#/blogs";
        engine.generate(Collections.singletonList(tempDir.resolve("blogs").toFile()), outDir.toFile(), "Blogify", true, "html", "primer", false, blogHome, null);

        Path indexFile = outDir.resolve("index.html");
        Path otherFile = outDir.resolve("other-guide.html");
        Path blogsJsonFile = outDir.resolve("blogs.json");

        assertTrue(Files.exists(indexFile), "index.html should exist");
        assertTrue(Files.exists(otherFile), "other-guide.html should exist");
        assertTrue(Files.exists(blogsJsonFile), "blogs.json should exist");

        String indexHtml = Files.readString(indexFile);
        // Header navigation should have "← Back to Blogs" linking to blogHome
        assertTrue(indexHtml.contains("href=\"http://localhost:3000/#/blogs\""), "index.html header should link to blogHome");
        assertTrue(indexHtml.contains("← Back to Blogs"), "index.html should have ← Back to Blogs text");
        // Option A: "Articles" link should NOT be present in nav
        assertFalse(indexHtml.contains("href=\"articles.html\">📚 Articles</a>"), "index.html should not have separate articles link when blogHome is active");

        String otherHtml = Files.readString(otherFile);
        // Breadcrumb should link to blogHome
        assertTrue(otherHtml.contains("<div class=\"breadcrumb\"><a href=\"http://localhost:3000/#/blogs\">← Back to Blogs</a></div>"), "other post breadcrumb should link to blogHome");

        // Verify blogs.json content
        String blogsJson = Files.readString(blogsJsonFile);
        assertTrue(blogsJson.contains("\"id\": \"blogify\""));
        assertTrue(blogsJson.contains("\"title\": \"Blogify Engine\""));
        assertTrue(blogsJson.contains("\"category\": \"Developer Tools\""));
        assertTrue(blogsJson.contains("\"link\": \"https://krushnatkhawale.github.io/blogify/\""));
        assertTrue(blogsJson.contains("\"tags\": [\"Java\", \"Gradle\"]"));

        assertTrue(blogsJson.contains("\"id\": \"other-guide\""));
        assertTrue(blogsJson.contains("\"category\": \"Architecture\""));
        assertTrue(blogsJson.contains("\"link\": \"https://krushnatkhawale.github.io/blogify/other-guide.html\""));
    }

    @Test
    void testGoogleAnalyticsAndVisitorCounterOptIn() throws IOException {
        Path blogDir = tempDir.resolve("blogs_analytics/test-post");
        Files.createDirectories(blogDir);
        String postMd = """
                ---
                title: "Analytics Guide"
                slug: "analytics-guide"
                home: true
                repoUrl: "https://github.com/krushnatkhawale/blogify"
                ---
                
                # Test Analytics
                Post content
                """;
        Files.writeString(blogDir.resolve("index.md"), postMd);

        Path outDir = tempDir.resolve("out_analytics");
        BlogifyEngine engine = new BlogifyEngine();

        // 1. Test opt-out / default behavior: no GA, no visitor counter
        engine.generate(Collections.singletonList(tempDir.resolve("blogs_analytics").toFile()),
                outDir.toFile(), "Blogify", true, "html", "primer", false, null, null, null, false, null);

        Path indexFile = outDir.resolve("index.html");
        String htmlOptOut = Files.readString(indexFile);
        assertFalse(htmlOptOut.contains("googletagmanager.com"), "GA4 script should not be present when not configured");
        assertFalse(htmlOptOut.contains("gtag("), "gtag should not be present when not configured");
        assertFalse(htmlOptOut.contains("blogify-views-badge"), "Visitor counter badge should not be present when disabled");
        assertFalse(htmlOptOut.contains("countapi.mileshilliard.com"), "Counter script should not be present when disabled");

        // 2. Test opt-in behavior: GA4 enabled and visitor counter enabled
        Path outDirOptIn = tempDir.resolve("out_analytics_optin");
        engine.generate(Collections.singletonList(tempDir.resolve("blogs_analytics").toFile()),
                outDirOptIn.toFile(), "Blogify", true, "html", "primer", false, null, null, "G-5BJKLL1EXD", true, null);

        Path indexOptInFile = outDirOptIn.resolve("index.html");
        String htmlOptIn = Files.readString(indexOptInFile);
        assertTrue(htmlOptIn.contains("https://www.googletagmanager.com/gtag/js?id=G-5BJKLL1EXD"), "GA4 script should be loaded");
        assertTrue(htmlOptIn.contains("gtag('config', 'G-5BJKLL1EXD', {"), "GA4 gtag config should be initialized with options");
        assertTrue(htmlOptIn.contains("page_path: window.location.pathname"), "GA4 should track page_path");
        assertTrue(htmlOptIn.contains("page_title: document.title"), "GA4 should track page_title");
        assertTrue(htmlOptIn.contains("id=\"blogify-views-badge\""), "Visitor counter badge should be rendered");
        assertTrue(htmlOptIn.contains("https://countapi.mileshilliard.com/api/v1/"), "CountAPI fetch should be injected");
        assertTrue(htmlOptIn.contains("var ns = \"krushnatkhawale-blogify\""), "Counter namespace should be derived from repoUrl");
    }
}

