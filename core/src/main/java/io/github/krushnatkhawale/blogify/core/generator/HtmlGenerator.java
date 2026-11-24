package io.github.krushnatkhawale.blogify.core.generator;

import io.github.krushnatkhawale.blogify.core.model.BlogCodeBlockModel;
import io.github.krushnatkhawale.blogify.core.model.BlogPostModel;
import io.github.krushnatkhawale.blogify.core.model.BlogSectionModel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates standalone static HTML and CSS pages, fully compatible with GitHub Pages (.nojekyll)
 * and all static file hosting platforms with zero external SSG build dependencies.
 *
 * Supports:
 * - 3 Switchable Themes: 'primer' (developer), 'editorial' (literary), 'minimal' (hacker)
 * - Dark and Light mode toggle with localStorage persistence
 * - Client-side Mermaid diagram rendering
 * - Story-first problem / insight / takeaway callouts
 * - Micro-snippet with expandable full-code drawer (<details>)
 * - Source version badge with commit SHA linking
 * - Retired / deprecated warning banners
 * - AI discoverability: llms.txt, llms-full.txt, robots.txt, and JSON-LD schema
 */
public class HtmlGenerator {

    private String lastGeneratedPortfolioJson = "";

    public String getLastGeneratedPortfolioJson() {
        return lastGeneratedPortfolioJson;
    }

    public List<Path> generate(File outputDir, String siteTitle, List<BlogPostModel> posts) throws IOException {
        return generate(outputDir, siteTitle, posts, "primer", false, null, null, null, false, null);
    }

    public List<Path> generate(File outputDir, String siteTitle, List<BlogPostModel> posts, String defaultTheme) throws IOException {
        return generate(outputDir, siteTitle, posts, defaultTheme, false, null, null, null, false, null);
    }

    public List<Path> generate(File outputDir, String siteTitle, List<BlogPostModel> posts, String defaultTheme, boolean includeDrafts) throws IOException {
        return generate(outputDir, siteTitle, posts, defaultTheme, includeDrafts, null, null, null, false, null);
    }

    public List<Path> generate(File outputDir, String siteTitle, List<BlogPostModel> posts, String defaultTheme, boolean includeDrafts, String blogHome, String siteUrl) throws IOException {
        return generate(outputDir, siteTitle, posts, defaultTheme, includeDrafts, blogHome, siteUrl, null, false, null);
    }

    public List<Path> generate(File outputDir, String siteTitle, List<BlogPostModel> posts, String defaultTheme, boolean includeDrafts, String blogHome, String siteUrl, String googleAnalyticsId, boolean enableVisitorCounter, String counterNamespace) throws IOException {
        List<Path> writtenFiles = new ArrayList<>();

        Path outPath = outputDir.toPath();
        Files.createDirectories(outPath);

        String title = (siteTitle != null && !siteTitle.isBlank()) ? siteTitle : "Project Blog";
        String theme = (defaultTheme != null && !defaultTheme.isBlank()) ? defaultTheme : "primer";

        List<BlogPostModel> publishablePosts = new ArrayList<>();
        for (BlogPostModel post : posts) {
            if (includeDrafts || !post.isDraft()) {
                publishablePosts.add(post);
            }
        }

        // 1. .nojekyll
        Path noJekyll = outPath.resolve(".nojekyll");
        Files.writeString(noJekyll, "# GitHub Pages: bypass Jekyll\n", StandardCharsets.UTF_8);
        writtenFiles.add(noJekyll);

        // 2. style.css
        Path cssFile = outPath.resolve("style.css");
        Files.writeString(cssFile, buildCss(), StandardCharsets.UTF_8);
        writtenFiles.add(cssFile);

        BlogPostModel homePost = null;
        for (BlogPostModel post : publishablePosts) {
            if (post.isHome() || "index".equalsIgnoreCase(post.getSlug())) {
                homePost = post;
                break;
            }
        }

        boolean hasMultipleArticles = publishablePosts.size() > 1;
        String resolvedNamespace = computeCounterNamespace(counterNamespace, title, (homePost != null ? homePost.getRepoUrl() : ""));

        if (homePost != null) {
            // 3. index.html renders the homePost
            Path indexFile = outPath.resolve("index.html");
            String indexHtml = buildPostHtml(title, homePost, theme, true, publishablePosts, hasMultipleArticles, blogHome, googleAnalyticsId, enableVisitorCounter, resolvedNamespace);
            Files.writeString(indexFile, indexHtml, StandardCharsets.UTF_8);
            writtenFiles.add(indexFile);

            // If homePost has a slug other than "index", also write <slug>.html
            if (!homePost.getSlug().isBlank() && !"index".equalsIgnoreCase(homePost.getSlug())) {
                Path slugFile = outPath.resolve(homePost.getSlug() + ".html");
                Files.writeString(slugFile, indexHtml, StandardCharsets.UTF_8);
                writtenFiles.add(slugFile);
            }

            // 4. articles.html for catalog of all posts
            if (hasMultipleArticles) {
                Path articlesFile = outPath.resolve("articles.html");
                String articlesHtml = buildIndexHtml(title, publishablePosts, theme, true, blogHome, googleAnalyticsId);
                Files.writeString(articlesFile, articlesHtml, StandardCharsets.UTF_8);
                writtenFiles.add(articlesFile);
            }

            // 5. <slug>.html for remaining posts
            for (BlogPostModel post : publishablePosts) {
                if (post != homePost) {
                    Path postFile = outPath.resolve(post.getSlug() + ".html");
                    String postHtml = buildPostHtml(title, post, theme, false, publishablePosts, hasMultipleArticles, blogHome, googleAnalyticsId, enableVisitorCounter, resolvedNamespace);
                    Files.writeString(postFile, postHtml, StandardCharsets.UTF_8);
                    writtenFiles.add(postFile);
                }
            }
        } else {
            // 3. Standard catalog index.html
            Path indexFile = outPath.resolve("index.html");
            String indexHtml = buildIndexHtml(title, publishablePosts, theme, false, blogHome, googleAnalyticsId);
            Files.writeString(indexFile, indexHtml, StandardCharsets.UTF_8);
            writtenFiles.add(indexFile);

            // 4. <slug>.html for each post
            for (BlogPostModel post : publishablePosts) {
                Path postFile = outPath.resolve(post.getSlug() + ".html");
                String postHtml = buildPostHtml(title, post, theme, false, publishablePosts, false, blogHome, googleAnalyticsId, enableVisitorCounter, resolvedNamespace);
                Files.writeString(postFile, postHtml, StandardCharsets.UTF_8);
                writtenFiles.add(postFile);
            }
        }

        // 5. llms.txt
        Path llmsFile = outPath.resolve("llms.txt");
        String llmsContent = buildLlmsTxt(title, publishablePosts, homePost);
        Files.writeString(llmsFile, llmsContent, StandardCharsets.UTF_8);
        writtenFiles.add(llmsFile);

        // 6. llms-full.txt
        Path llmsFullFile = outPath.resolve("llms-full.txt");
        String llmsFullContent = buildLlmsFullTxt(title, publishablePosts);
        Files.writeString(llmsFullFile, llmsFullContent, StandardCharsets.UTF_8);
        writtenFiles.add(llmsFullFile);

        // 7. robots.txt
        Path robotsFile = outPath.resolve("robots.txt");
        String robotsContent = buildRobotsTxt();
        Files.writeString(robotsFile, robotsContent, StandardCharsets.UTF_8);
        writtenFiles.add(robotsFile);

        // 8. blogs.json (Portfolio cards feed)
        Path blogsJsonFile = outPath.resolve("blogs.json");
        this.lastGeneratedPortfolioJson = buildPortfolioBlogsJson(publishablePosts, siteUrl, homePost);
        Files.writeString(blogsJsonFile, this.lastGeneratedPortfolioJson, StandardCharsets.UTF_8);
        writtenFiles.add(blogsJsonFile);

        return writtenFiles;
    }

    private String buildIndexHtml(String siteTitle, List<BlogPostModel> posts, String defaultTheme) {
        return buildIndexHtml(siteTitle, posts, defaultTheme, false, null);
    }

    private String buildIndexHtml(String siteTitle, List<BlogPostModel> posts, String defaultTheme, boolean hasHomePage) {
        return buildIndexHtml(siteTitle, posts, defaultTheme, hasHomePage, null);
    }

    private String buildIndexHtml(String siteTitle, List<BlogPostModel> posts, String defaultTheme, boolean hasHomePage, String blogHome) {
        return buildIndexHtml(siteTitle, posts, defaultTheme, hasHomePage, blogHome, null);
    }

    private String buildIndexHtml(String siteTitle, List<BlogPostModel> posts, String defaultTheme, boolean hasHomePage, String blogHome, String googleAnalyticsId) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\" data-theme=\"").append(escapeHtml(defaultTheme)).append("\" data-mode=\"light\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(escapeHtml(siteTitle)).append(hasHomePage ? " - All Articles" : "").append("</title>\n");
        sb.append("  <link rel=\"stylesheet\" href=\"style.css\">\n");
        sb.append("  <link rel=\"alternate\" type=\"text/plain\" href=\"llms.txt\" title=\"LLM Knowledge Base\">\n");
        sb.append(buildThemeInitScript(defaultTheme));
        sb.append(buildGoogleAnalyticsScript(googleAnalyticsId));
        sb.append("</head>\n<body>\n");

        sb.append(buildHeaderHtml(siteTitle, false, hasHomePage, blogHome));

        sb.append("  <main>\n");
        sb.append("    <section class=\"hero\">\n");
        sb.append("      <h1>").append(escapeHtml(siteTitle)).append(hasHomePage ? " Articles" : "").append("</h1>\n");
        if (hasHomePage) {
            String backHref = (blogHome != null && !blogHome.isBlank()) ? blogHome : "index.html";
            String backText = (blogHome != null && !blogHome.isBlank()) ? "← Back to Blogs" : "← Back to Home";
            sb.append("      <p class=\"hero-subtitle\">Complete directory of technical articles and architecture specifications. <a href=\"").append(escapeHtml(backHref)).append("\">").append(escapeHtml(backText)).append("</a></p>\n");
        } else {
            sb.append("      <p class=\"hero-subtitle\">Automated static technical blog generated directly from application source code and markdown bundles.</p>\n");
        }
        sb.append("      <div class=\"ai-badge-row\">\n");
        sb.append("        <a href=\"llms.txt\" class=\"ai-link-badge\" title=\"Formatted knowledge feed for AI agents and LLMs\">🤖 llms.txt</a>\n");
        sb.append("        <a href=\"llms-full.txt\" class=\"ai-link-badge\" title=\"Full corpus for LLM ingest\">📄 llms-full.txt</a>\n");
        sb.append("      </div>\n");
        sb.append("    </section>\n\n");

        if (posts.isEmpty()) {
            sb.append("    <p><em>No blog posts found. Add markdown articles to <code>blogs/&lt;topic&gt;/index.md</code> or annotate Java methods with <code>@blog.*</code> Javadoc tags.</em></p>\n");
        } else {
            List<BlogPostModel> activePosts = new ArrayList<>();
            List<BlogPostModel> retiredPosts = new ArrayList<>();

            for (BlogPostModel post : posts) {
                if (post.isRetired()) {
                    retiredPosts.add(post);
                } else {
                    activePosts.add(post);
                }
            }

            // Active articles
            for (BlogPostModel post : activePosts) {
                renderPostCard(sb, post);
            }

            // Retired / Archived section if any
            if (!retiredPosts.isEmpty()) {
                sb.append("    <section class=\"archived-section\">\n");
                sb.append("      <h2 class=\"archived-heading\">📦 Archived & Retired Architecture Guides</h2>\n");
                sb.append("      <p class=\"archived-desc\">The following articles describe older system iterations retained for immutable version tracking.</p>\n");
                for (BlogPostModel post : retiredPosts) {
                    renderPostCard(sb, post);
                }
                sb.append("    </section>\n");
            }
        }

        sb.append("  </main>\n\n");
        sb.append(buildFooterHtml());
        sb.append(buildThemeSwitcherScript());
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    private void renderPostCard(StringBuilder sb, BlogPostModel post) {
        String postHref = post.isHome() ? "index.html" : post.getSlug() + ".html";
        sb.append("    <div class=\"post-card").append(post.isRetired() ? " post-card-retired" : "").append("\">\n");
        sb.append("      <div class=\"post-card-header\">\n");
        sb.append("        <h2 class=\"post-title\"><a href=\"").append(escapeHtml(postHref)).append("\">");
        sb.append(escapeHtml(post.getTitle())).append("</a></h2>\n");
        if (post.isHome()) {
            sb.append("        <span class=\"badge-home\">MAIN</span>\n");
        } else if (post.isRetired()) {
            sb.append("        <span class=\"badge-retired\">RETIRED</span>\n");
        }
        sb.append("      </div>\n");

        sb.append("      <div class=\"post-meta\">\n");
        if (!post.getDate().isBlank()) {
            sb.append("        <span>📅 ").append(escapeHtml(post.getDate())).append("</span>\n");
        }
        if (!post.getAuthor().isBlank()) {
            sb.append("        <span>•</span><span>✍️ ").append(escapeHtml(post.getAuthor())).append("</span>\n");
        }
        if (!post.getVersion().isBlank() || !post.getCommitSha().isBlank()) {
            sb.append("        <span>•</span><span class=\"meta-source\">Source: ");
            if (!post.getVersion().isBlank()) {
                sb.append("<strong>").append(escapeHtml(post.getVersion())).append("</strong> ");
            }
            if (!post.getCommitSha().isBlank()) {
                sb.append("(").append(escapeHtml(post.getCommitSha())).append(")");
            }
            sb.append("</span>\n");
        }
        sb.append("      </div>\n");

        if (!post.getDescription().isBlank()) {
            sb.append("      <p class=\"post-desc\">").append(escapeHtml(post.getDescription())).append("</p>\n");
        }

        if (!post.getTags().isEmpty()) {
            sb.append("      <div class=\"tag-list\">\n");
            for (String tag : post.getTags()) {
                sb.append("        <span class=\"tag\">#").append(escapeHtml(tag)).append("</span>\n");
            }
            sb.append("      </div>\n");
        }
        sb.append("    </div>\n\n");
    }

    private String buildPostHtml(String siteTitle, BlogPostModel post, String defaultTheme) {
        return buildPostHtml(siteTitle, post, defaultTheme, false, Collections.emptyList(), false, null);
    }

    private String buildPostHtml(String siteTitle, BlogPostModel post, String defaultTheme, boolean isHomePage, List<BlogPostModel> allPosts, boolean hasMultipleArticles) {
        return buildPostHtml(siteTitle, post, defaultTheme, isHomePage, allPosts, hasMultipleArticles, null);
    }

    private String buildPostHtml(String siteTitle, BlogPostModel post, String defaultTheme, boolean isHomePage, List<BlogPostModel> allPosts, boolean hasMultipleArticles, String blogHome) {
        return buildPostHtml(siteTitle, post, defaultTheme, isHomePage, allPosts, hasMultipleArticles, blogHome, null, false, null);
    }

    private String buildPostHtml(String siteTitle, BlogPostModel post, String defaultTheme, boolean isHomePage, List<BlogPostModel> allPosts, boolean hasMultipleArticles, String blogHome, String googleAnalyticsId, boolean enableVisitorCounter, String counterNamespace) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\" data-theme=\"").append(escapeHtml(defaultTheme)).append("\" data-mode=\"light\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(escapeHtml(post.getTitle())).append(" - ").append(escapeHtml(siteTitle)).append("</title>\n");
        sb.append("  <link rel=\"stylesheet\" href=\"style.css\">\n");
        sb.append(buildJsonLd(siteTitle, post));
        sb.append(buildThemeInitScript(defaultTheme));
        sb.append(buildGoogleAnalyticsScript(googleAnalyticsId));

        // Include Mermaid.js for architecture diagrams
        sb.append("  <script type=\"module\">\n");
        sb.append("    import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';\n");
        sb.append("    mermaid.initialize({ startOnLoad: true, theme: 'default' });\n");
        sb.append("  </script>\n");

        sb.append("</head>\n<body>\n");

        sb.append(buildHeaderHtml(siteTitle, isHomePage, hasMultipleArticles, blogHome));

        sb.append("  <article>\n");
        if (!isHomePage) {
            if (blogHome != null && !blogHome.isBlank()) {
                sb.append("    <div class=\"breadcrumb\"><a href=\"").append(escapeHtml(blogHome)).append("\">← Back to Blogs</a></div>\n");
            } else if (hasMultipleArticles) {
                sb.append("    <div class=\"breadcrumb\"><a href=\"index.html\">← Home</a> <span class=\"breadcrumb-sep\">/</span> <a href=\"articles.html\">All Articles</a></div>\n");
            } else {
                sb.append("    <div class=\"breadcrumb\"><a href=\"index.html\">← All Articles</a></div>\n");
            }
        }

        // Retired alert banner if post is retired
        if (post.isRetired()) {
            sb.append("    <div class=\"callout callout-warning callout-retired\">\n");
            sb.append("      <div class=\"callout-header\">⚠️ Retired / Superseded Article</div>\n");
            sb.append("      <div class=\"callout-body\">\n");
            sb.append("        <p>This article documents a previous version of the architecture and is preserved for historical reference.</p>\n");
            if (!post.getSupersededBy().isBlank()) {
                sb.append("        <p><strong>Current Version:</strong> <a href=\"").append(escapeHtml(post.getSupersededBy())).append(".html\">View updated guide →</a></p>\n");
            }
            if (!post.getSupersededMessage().isBlank()) {
                sb.append("        <p><em>").append(escapeHtml(post.getSupersededMessage())).append("</em></p>\n");
            }
            sb.append("      </div>\n");
            sb.append("    </div>\n\n");
        }

        sb.append("    <h1>").append(escapeHtml(post.getTitle())).append("</h1>\n\n");

        // Meta Bar with Version Badge & Visitor Counter
        sb.append("    <div class=\"post-meta-bar\">\n");
        if (!post.getAuthor().isBlank()) {
            sb.append("      <span>✍️ <strong>").append(escapeHtml(post.getAuthor())).append("</strong></span>\n");
        }
        if (!post.getDate().isBlank()) {
            sb.append("      <span>📅 ").append(escapeHtml(post.getDate())).append("</span>\n");
        }
        if (enableVisitorCounter) {
            sb.append("      <span class=\"visitor-counter-badge\" id=\"blogify-views-badge\" style=\"display:none;\">👁️ <strong id=\"blogify-views-count\">...</strong> views</span>\n");
        }

        // Subtle Source Code Version Badge
        if (!post.getVersion().isBlank() || !post.getCommitSha().isBlank()) {
            sb.append("      <span class=\"source-version-badge\">📦 Source: ");
            if (!post.getVersion().isBlank()) {
                sb.append("<strong>").append(escapeHtml(post.getVersion())).append("</strong> ");
            }
            if (!post.getCommitSha().isBlank()) {
                if (!post.getRepoUrl().isBlank()) {
                    String commitUrl = post.getRepoUrl().replaceAll("/+$", "") + "/commit/" + post.getCommitSha();
                    sb.append("· <a href=\"").append(escapeHtml(commitUrl)).append("\" target=\"_blank\" rel=\"noopener\" class=\"commit-link\">commit ").append(escapeHtml(post.getCommitSha())).append(" ↗</a>");
                } else {
                    sb.append("· commit <code>").append(escapeHtml(post.getCommitSha())).append("</code>");
                }
            }
            sb.append("</span>\n");
        }

        if (!post.getTags().isEmpty()) {
            sb.append("      <div class=\"tags-container\">\n");
            for (String tag : post.getTags()) {
                sb.append("        <span class=\"tag\">#").append(escapeHtml(tag)).append("</span>\n");
            }
            sb.append("      </div>\n");
        }
        sb.append("    </div>\n\n");

        // Story-First: The Problem
        if (!post.getProblem().isBlank()) {
            sb.append("    <div class=\"callout callout-problem\">\n");
            sb.append("      <div class=\"callout-header\">⚠️ The Problem</div>\n");
            sb.append("      <div class=\"callout-body\">\n").append(renderCalloutBody(post.getProblem())).append("      </div>\n");
            sb.append("    </div>\n\n");
        }

        // Story-First: The Insight
        if (!post.getInsight().isBlank()) {
            sb.append("    <div class=\"callout callout-insight\">\n");
            sb.append("      <div class=\"callout-header\">💡 The Core Insight</div>\n");
            sb.append("      <div class=\"callout-body\">\n").append(renderCalloutBody(post.getInsight())).append("      </div>\n");
            sb.append("    </div>\n\n");
        }

        // Architecture Diagram (Mermaid)
        if (!post.getDiagram().isBlank()) {
            sb.append("    <div class=\"diagram-card\">\n");
            sb.append("      <div class=\"diagram-header\">📐 Architecture Workflow</div>\n");
            sb.append("      <div class=\"mermaid\">\n").append(post.getDiagram().trim()).append("\n</div>\n");
            sb.append("    </div>\n\n");
        }

        // Intro / Description
        if (!post.getIntroContent().isBlank()) {
            sb.append("    <div class=\"intro-box\">\n");
            sb.append("      ").append(escapeHtml(post.getIntroContent()).replace("\n", "<br>\n      ")).append("\n");
            sb.append("    </div>\n\n");
        }

        // Markdown Raw Body (if loaded from blogs/<slug>/index.md)
        if (!post.getRawBody().isBlank()) {
            sb.append(renderMarkdownToHtml(post.getRawBody()));
        }

        // Standalone code blocks
        for (BlogCodeBlockModel cb : post.getStandaloneCodeBlocks()) {
            appendHtmlCodeBlock(sb, cb);
        }

        // Sections
        for (BlogSectionModel section : post.getSections()) {
            sb.append("    <h2>").append(escapeHtml(section.getHeading())).append("</h2>\n");
            if (!section.getContent().isBlank()) {
                sb.append("    <p>").append(escapeHtml(section.getContent()).replace("\n", "<br>\n    ")).append("</p>\n");
            }
            for (BlogCodeBlockModel cb : section.getCodeBlocks()) {
                appendHtmlCodeBlock(sb, cb);
            }
        }

        // Story-First: Key Takeaways
        if (!post.getTakeaways().isEmpty()) {
            sb.append("    <div class=\"callout callout-takeaways\">\n");
            sb.append("      <div class=\"callout-header\">🚀 Key Takeaways</div>\n");
            sb.append("      <ul class=\"takeaways-list\">\n");
            for (String takeaway : post.getTakeaways()) {
                sb.append("        <li>").append(escapeHtml(takeaway)).append("</li>\n");
            }
            sb.append("      </ul>\n");
            sb.append("    </div>\n\n");
        }

        // If this is the home page and there are other articles, render a "More Articles" section
        if (isHomePage && hasMultipleArticles) {
            sb.append("    <section class=\"home-more-articles\">\n");
            sb.append("      <div class=\"more-articles-header\">\n");
            sb.append("        <h2>📚 More Architecture Guides & Articles</h2>\n");
            sb.append("        <a href=\"articles.html\" class=\"view-all-link\">Browse all articles →</a>\n");
            sb.append("      </div>\n");
            sb.append("      <div class=\"more-articles-grid\">\n");
            int count = 0;
            for (BlogPostModel other : allPosts) {
                if (other != post && count < 4) {
                    renderMiniPostCard(sb, other);
                    count++;
                }
            }
            sb.append("      </div>\n");
            sb.append("    </section>\n\n");
        }

        sb.append("  </article>\n\n");
        sb.append(buildFooterHtml());
        sb.append(buildThemeSwitcherScript());
        if (enableVisitorCounter) {
            String slug = (post.getSlug() != null && !post.getSlug().isBlank()) ? post.getSlug() : "home";
            sb.append(buildVisitorCounterScript(counterNamespace, slug));
        }
        sb.append("</body>\n</html>");

        return sb.toString();
    }

    private void renderMiniPostCard(StringBuilder sb, BlogPostModel post) {
        String postHref = post.isHome() ? "index.html" : post.getSlug() + ".html";
        sb.append("        <div class=\"mini-card").append(post.isRetired() ? " post-card-retired" : "").append("\">\n");
        sb.append("          <h3><a href=\"").append(escapeHtml(postHref)).append("\">").append(escapeHtml(post.getTitle())).append("</a></h3>\n");
        if (!post.getDescription().isBlank()) {
            sb.append("          <p class=\"mini-card-desc\">").append(escapeHtml(post.getDescription())).append("</p>\n");
        }
        sb.append("          <div class=\"mini-card-meta\">");
        if (!post.getDate().isBlank()) {
            sb.append("<span>📅 ").append(escapeHtml(post.getDate())).append("</span>");
        }
        if (!post.getVersion().isBlank()) {
            sb.append(" • <span>v").append(escapeHtml(post.getVersion())).append("</span>");
        }
        sb.append("</div>\n");
        sb.append("        </div>\n");
    }

    private void appendHtmlCodeBlock(StringBuilder sb, BlogCodeBlockModel cb) {
        sb.append("    <div class=\"code-wrapper\">\n");
        sb.append("      <div class=\"code-header\">\n");
        sb.append("        <span class=\"code-title\">").append(escapeHtml(cb.getDescription())).append("</span>\n");
        sb.append("        <span class=\"code-lang\">").append(escapeHtml(cb.getLanguage().toUpperCase())).append("</span>\n");
        sb.append("      </div>\n");
        sb.append("      <pre><code>").append(highlightJavaCode(cb.getCode())).append("</code></pre>\n");

        // Collapsible Full Code Drawer
        if (cb.hasFullCode()) {
            long lines = cb.getFullCode().lines().count();
            sb.append("      <details class=\"code-drawer\">\n");
            sb.append("        <summary class=\"code-drawer-summary\">🔍 Expand complete implementation (").append(lines).append(" lines)</summary>\n");
            sb.append("        <pre class=\"drawer-code\"><code>").append(highlightJavaCode(cb.getFullCode())).append("</code></pre>\n");
            sb.append("      </details>\n");
        }

        sb.append("    </div>\n\n");
    }

    private String buildHeaderHtml(String siteTitle, boolean isHome) {
        return buildHeaderHtml(siteTitle, isHome, false, null);
    }

    private String buildHeaderHtml(String siteTitle, boolean isHome, boolean hasArticlesPage) {
        return buildHeaderHtml(siteTitle, isHome, hasArticlesPage, null);
    }

    private String buildHeaderHtml(String siteTitle, boolean isHome, boolean hasArticlesPage, String blogHome) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <header>\n");
        sb.append("    <div class=\"header-container\">\n");
        String brandHref = (blogHome != null && !blogHome.isBlank()) ? blogHome : "index.html";
        sb.append("      <a href=\"").append(escapeHtml(brandHref)).append("\" class=\"brand-title\">⚡ ").append(escapeHtml(siteTitle));
        sb.append(" <span class=\"badge-compile-time\">COMPILED</span></a>\n");
        sb.append("      <div class=\"header-actions\">\n");
        sb.append("        <nav class=\"nav-links\">\n");
        if (blogHome != null && !blogHome.isBlank()) {
            sb.append("          <a href=\"").append(escapeHtml(blogHome)).append("\">← Back to Blogs</a>\n");
        } else if (hasArticlesPage) {
            if (isHome) {
                sb.append("          <a href=\"articles.html\">📚 Articles</a>\n");
            } else {
                sb.append("          <a href=\"index.html\">← Home</a>\n");
                sb.append("          <a href=\"articles.html\">Articles</a>\n");
            }
        } else if (!isHome) {
            sb.append("          <a href=\"index.html\">← Articles</a>\n");
        }
        sb.append("          <a href=\"llms.txt\" class=\"nav-ai-link\" title=\"AI Discoverability Feed\">AI llms.txt</a>\n");
        sb.append("        </nav>\n");
        sb.append("        <div class=\"theme-controls\">\n");
        sb.append("          <select id=\"theme-select\" class=\"theme-dropdown\" aria-label=\"Theme Switcher\" onchange=\"switchBlogTheme(this.value)\">\n");
        sb.append("            <option value=\"primer\">Primer</option>\n");
        sb.append("            <option value=\"editorial\">Editorial</option>\n");
        sb.append("            <option value=\"minimal\">Minimal</option>\n");
        sb.append("          </select>\n");
        sb.append("          <button id=\"mode-toggle-btn\" class=\"mode-btn\" aria-label=\"Toggle Light/Dark Mode\" onclick=\"toggleColorMode()\" title=\"Toggle dark/light mode\">🌓</button>\n");
        sb.append("        </div>\n");
        sb.append("      </div>\n");
        sb.append("    </div>\n");
        sb.append("  </header>\n\n");
        return sb.toString();
    }

    private String buildFooterHtml() {
        return """
                  <footer>
                    <div class="footer-container">
                      <p>Generated by <strong>Blogify</strong> • Zero runtime dependencies • Ready for GitHub Pages</p>
                      <p class="footer-ai-note">AI Discoverability enabled: <a href="llms.txt">llms.txt</a> &bull; <a href="llms-full.txt">llms-full.txt</a></p>
                    </div>
                  </footer>
                """;
    }

    private String buildThemeInitScript(String defaultTheme) {
        return "  <script>\n" +
                "    (function() {\n" +
                "      var savedTheme = localStorage.getItem('blogify-theme') || '" + escapeHtml(defaultTheme) + "';\n" +
                "      var savedMode = localStorage.getItem('blogify-mode') || (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light');\n" +
                "      document.documentElement.setAttribute('data-theme', savedTheme);\n" +
                "      document.documentElement.setAttribute('data-mode', savedMode);\n" +
                "    })();\n" +
                "  </script>\n";
    }

    private String buildThemeSwitcherScript() {
        return """
                  <script>
                    function switchBlogTheme(theme) {
                      document.documentElement.setAttribute('data-theme', theme);
                      localStorage.setItem('blogify-theme', theme);
                    }
                    function toggleColorMode() {
                      var cur = document.documentElement.getAttribute('data-mode') || 'light';
                      var next = cur === 'dark' ? 'light' : 'dark';
                      document.documentElement.setAttribute('data-mode', next);
                      localStorage.setItem('blogify-mode', next);
                    }
                    document.addEventListener('DOMContentLoaded', function() {
                      var curTheme = document.documentElement.getAttribute('data-theme') || 'primer';
                      var sel = document.getElementById('theme-select');
                      if (sel) sel.value = curTheme;

                      // Auto-playing Carousel Controller
                      document.querySelectorAll('.carousel-container').forEach(function(carousel) {
                        var slides = carousel.querySelectorAll('.carousel-slide');
                        var dots = carousel.querySelectorAll('.carousel-dot');
                        var prevBtn = carousel.querySelector('.carousel-prev');
                        var nextBtn = carousel.querySelector('.carousel-next');
                        if (slides.length <= 1) {
                          if (prevBtn) prevBtn.style.display = 'none';
                          if (nextBtn) nextBtn.style.display = 'none';
                          var dotsWrap = carousel.querySelector('.carousel-dots');
                          if (dotsWrap) dotsWrap.style.display = 'none';
                          return;
                        }
                        var curIdx = 0;
                        var autoplayInterval = parseInt(carousel.getAttribute('data-autoplay') || '4000', 10);
                        var timer = null;

                        function showSlide(idx) {
                          if (idx < 0) idx = slides.length - 1;
                          if (idx >= slides.length) idx = 0;
                          curIdx = idx;
                          slides.forEach(function(s, i) {
                            if (i === curIdx) {
                              s.classList.add('active');
                            } else {
                              s.classList.remove('active');
                            }
                          });
                          dots.forEach(function(d, i) {
                            if (i === curIdx) {
                              d.classList.add('active');
                            } else {
                              d.classList.remove('active');
                            }
                          });
                        }

                        function startTimer() {
                          if (autoplayInterval > 0 && !timer) {
                            timer = setInterval(function() {
                              showSlide(curIdx + 1);
                            }, autoplayInterval);
                          }
                        }

                        function stopTimer() {
                          if (timer) {
                            clearInterval(timer);
                            timer = null;
                          }
                        }

                        if (prevBtn) {
                          prevBtn.addEventListener('click', function(e) {
                            e.preventDefault();
                            showSlide(curIdx - 1);
                          });
                        }

                        if (nextBtn) {
                          nextBtn.addEventListener('click', function(e) {
                            e.preventDefault();
                            showSlide(curIdx + 1);
                          });
                        }

                        dots.forEach(function(dot) {
                          dot.addEventListener('click', function(e) {
                            e.preventDefault();
                            var i = parseInt(dot.getAttribute('data-index'), 10);
                            showSlide(i);
                          });
                        });

                        carousel.addEventListener('mouseenter', stopTimer);
                        carousel.addEventListener('mouseleave', startTimer);
                        carousel.addEventListener('touchstart', stopTimer, { passive: true });
                        carousel.addEventListener('touchend', startTimer, { passive: true });

                        startTimer();
                      });
                    });
                  </script>
                """;
    }

    private String buildJsonLd(String siteTitle, BlogPostModel post) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <script type=\"application/ld+json\">\n  {\n");
        sb.append("    \"@context\": \"https://schema.org\",\n");
        sb.append("    \"@type\": \"TechArticle\",\n");
        sb.append("    \"headline\": \"").append(escapeJson(post.getTitle())).append("\",\n");
        sb.append("    \"description\": \"").append(escapeJson(post.getDescription())).append("\",\n");
        if (!post.getAuthor().isBlank()) {
            sb.append("    \"author\": { \"@type\": \"Person\", \"name\": \"").append(escapeJson(post.getAuthor())).append("\" },\n");
        }
        if (!post.getDate().isBlank()) {
            sb.append("    \"datePublished\": \"").append(escapeJson(post.getDate())).append("\",\n");
        }
        if (!post.getVersion().isBlank()) {
            sb.append("    \"version\": \"").append(escapeJson(post.getVersion())).append("\",\n");
        }
        sb.append("    \"publisher\": { \"@type\": \"Organization\", \"name\": \"").append(escapeJson(siteTitle)).append("\" }\n");
        sb.append("  }\n  </script>\n");
        return sb.toString();
    }

    private String buildLlmsTxt(String siteTitle, List<BlogPostModel> posts) {
        return buildLlmsTxt(siteTitle, posts, null);
    }

    private String buildLlmsTxt(String siteTitle, List<BlogPostModel> posts, BlogPostModel homePost) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(siteTitle).append("\n\n");
        sb.append("> Curated technical articles and architecture specifications generated directly from codebase and documentation.\n\n");
        sb.append("## Available Guides\n\n");

        for (BlogPostModel post : posts) {
            String href = (homePost != null && (post == homePost || post.isHome())) ? "index.html" : post.getSlug() + ".html";
            sb.append("- [").append(post.getTitle()).append("](").append(href).append("): ");
            sb.append(post.getDescription().isBlank() ? post.getTitle() : post.getDescription());
            if (!post.getVersion().isBlank()) {
                sb.append(" (Version: ").append(post.getVersion()).append(")");
            }
            if (post.isRetired()) {
                sb.append(" [Status: RETIRED]");
            }
            sb.append("\n");
        }

        sb.append("\n## Architectural Takeaways\n\n");
        for (BlogPostModel post : posts) {
            if (!post.getTakeaways().isEmpty()) {
                sb.append("### ").append(post.getTitle()).append("\n");
                for (String t : post.getTakeaways()) {
                    sb.append("- ").append(t).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String buildLlmsFullTxt(String siteTitle, List<BlogPostModel> posts) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(siteTitle).append(" - Full Corpus\n\n");

        for (BlogPostModel post : posts) {
            sb.append("---\n\n");
            sb.append("# ").append(post.getTitle()).append("\n\n");
            if (!post.getDescription().isBlank()) {
                sb.append("> ").append(post.getDescription()).append("\n\n");
            }
            if (!post.getVersion().isBlank() || !post.getCommitSha().isBlank()) {
                sb.append("**Source Version:** ").append(post.getVersion());
                if (!post.getCommitSha().isBlank()) {
                    sb.append(" (commit ").append(post.getCommitSha()).append(")");
                }
                sb.append("\n\n");
            }
            if (!post.getProblem().isBlank()) {
                sb.append("## The Problem\n").append(post.getProblem()).append("\n\n");
            }
            if (!post.getInsight().isBlank()) {
                sb.append("## The Insight\n").append(post.getInsight()).append("\n\n");
            }
            if (!post.getRawBody().isBlank()) {
                sb.append(post.getRawBody()).append("\n\n");
            }
            for (BlogSectionModel sec : post.getSections()) {
                sb.append("## ").append(sec.getHeading()).append("\n\n");
                if (!sec.getContent().isBlank()) {
                    sb.append(sec.getContent()).append("\n\n");
                }
                for (BlogCodeBlockModel cb : sec.getCodeBlocks()) {
                    sb.append("```").append(cb.getLanguage()).append("\n");
                    sb.append(cb.getCode()).append("\n```\n\n");
                }
            }
            if (!post.getTakeaways().isEmpty()) {
                sb.append("## Takeaways\n");
                for (String t : post.getTakeaways()) {
                    sb.append("- ").append(t).append("\n");
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    private String buildRobotsTxt() {
        return """
                User-agent: *
                Allow: /

                User-agent: GPTBot
                Allow: /

                User-agent: ClaudeBot
                Allow: /

                User-agent: PerplexityBot
                Allow: /

                User-agent: Applebot-Extended
                Allow: /

                Sitemap: /llms.txt
                """;
    }

    private static final Pattern IMG_BLOCK_PATTERN = Pattern.compile("^!\\[([^\\]]*)\\]\\(([^\\)]+)\\)$");

    private String renderCarousel(List<String[]> images) {
        if (images.isEmpty()) return "";
        if (images.size() == 1) {
            String alt = images.get(0)[0];
            String src = images.get(0)[1];
            return "    <div class=\"img-wrapper\"><img src=\"" + escapeHtml(src) + "\" alt=\"" + escapeHtml(alt) + "\" class=\"md-img\"></div>\n\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("    <div class=\"carousel-container\" data-autoplay=\"4000\">\n");
        sb.append("      <div class=\"carousel-track\">\n");
        for (int idx = 0; idx < images.size(); idx++) {
            String alt = images.get(idx)[0];
            String src = images.get(idx)[1];
            String activeClass = (idx == 0) ? " active" : "";
            sb.append("        <div class=\"carousel-slide").append(activeClass).append("\">\n");
            sb.append("          <img src=\"").append(escapeHtml(src)).append("\" alt=\"").append(escapeHtml(alt)).append("\" class=\"carousel-img\">\n");
            if (!alt.isBlank()) {
                sb.append("          <div class=\"carousel-caption\">").append(escapeHtml(alt)).append("</div>\n");
            }
            sb.append("        </div>\n");
        }
        sb.append("      </div>\n");
        sb.append("      <button class=\"carousel-prev\" aria-label=\"Previous slide\">&#10094;</button>\n");
        sb.append("      <button class=\"carousel-next\" aria-label=\"Next slide\">&#10095;</button>\n");
        sb.append("      <div class=\"carousel-dots\">\n");
        for (int idx = 0; idx < images.size(); idx++) {
            String activeDot = (idx == 0) ? " active" : "";
            sb.append("        <span class=\"carousel-dot").append(activeDot).append("\" data-index=\"").append(idx).append("\"></span>\n");
        }
        sb.append("      </div>\n");
        sb.append("    </div>\n\n");
        return sb.toString();
    }

    private String renderMarkdownToHtml(String md) {
        if (md == null || md.isBlank()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = md.split("\r?\n");
        boolean inCode = false;
        String codeLang = "";
        StringBuilder codeContent = new StringBuilder();

        boolean inList = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.startsWith("```")) {
                if (inList) {
                    sb.append("    </ul>\n");
                    inList = false;
                }
                if (!inCode) {
                    inCode = true;
                    codeLang = line.substring(3).trim();
                    codeContent.setLength(0);
                } else {
                    inCode = false;
                    if ("mermaid".equalsIgnoreCase(codeLang)) {
                        sb.append("    <div class=\"diagram-card\"><div class=\"mermaid\">\n");
                        sb.append(codeContent.toString());
                        sb.append("    </div></div>\n\n");
                    } else if ("carousel".equalsIgnoreCase(codeLang)) {
                        List<String[]> carouselImages = new ArrayList<>();
                        for (String cLine : codeContent.toString().split("\r?\n")) {
                            Matcher m = IMG_BLOCK_PATTERN.matcher(cLine.trim());
                            if (m.matches()) {
                                carouselImages.add(new String[]{m.group(1), m.group(2)});
                            }
                        }
                        sb.append(renderCarousel(carouselImages));
                    } else {
                        sb.append("    <div class=\"code-wrapper\">\n");
                        if (!codeLang.isBlank()) {
                            sb.append("      <div class=\"code-header\"><span class=\"code-lang\">").append(escapeHtml(codeLang.toUpperCase())).append("</span></div>\n");
                        }
                        sb.append("      <pre><code>").append(highlightJavaCode(codeContent.toString())).append("</code></pre>\n");
                        sb.append("    </div>\n\n");
                    }
                }
                continue;
            }

            if (inCode) {
                codeContent.append(line).append("\n");
                continue;
            }

            // Check for standalone or consecutive image lines
            Matcher imgMatcher = IMG_BLOCK_PATTERN.matcher(line.trim());
            if (imgMatcher.matches()) {
                if (inList) {
                    sb.append("    </ul>\n");
                    inList = false;
                }
                List<String[]> consecutiveImages = new ArrayList<>();
                consecutiveImages.add(new String[]{imgMatcher.group(1), imgMatcher.group(2)});
                while (i + 1 < lines.length) {
                    Matcher nextMatcher = IMG_BLOCK_PATTERN.matcher(lines[i + 1].trim());
                    if (nextMatcher.matches()) {
                        consecutiveImages.add(new String[]{nextMatcher.group(1), nextMatcher.group(2)});
                        i++;
                    } else {
                        break;
                    }
                }
                sb.append(renderCarousel(consecutiveImages));
                continue;
            }

            // Headings and block formatting
            if (line.startsWith("- ") || line.startsWith("* ")) {
                if (!inList) {
                    sb.append("    <ul class=\"md-ul\">\n");
                    inList = true;
                }
                sb.append("      <li class=\"md-li\">").append(formatInlineMd(line.substring(2))).append("</li>\n");
            } else {
                if (inList) {
                    sb.append("    </ul>\n");
                    inList = false;
                }
                if (line.startsWith("### ")) {
                    sb.append("    <h3>").append(formatInlineMd(line.substring(4))).append("</h3>\n");
                } else if (line.startsWith("## ")) {
                    sb.append("    <h2>").append(formatInlineMd(line.substring(3))).append("</h2>\n");
                } else if (line.startsWith("# ")) {
                    sb.append("    <h1>").append(formatInlineMd(line.substring(2))).append("</h1>\n");
                } else if (line.startsWith("> ")) {
                    sb.append("    <blockquote class=\"md-quote\">").append(formatInlineMd(line.substring(2))).append("</blockquote>\n");
                } else if (line.isBlank()) {
                    sb.append("\n");
                } else {
                    sb.append("    <p>").append(formatInlineMd(line)).append("</p>\n");
                }
            }
        }

        if (inList) {
            sb.append("    </ul>\n");
        }

        return sb.toString();
    }

    private String formatInlineMd(String text) {
        String escaped = escapeHtml(text);
        // Images: ![alt](url)
        escaped = escaped.replaceAll("!\\[([^\\]]*)\\]\\(([^\\)]+)\\)", "<div class=\"img-wrapper\"><img src=\"$2\" alt=\"$1\" class=\"md-img\"></div>");
        // Bold: **text**
        escaped = escaped.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        // Inline code: `code`
        escaped = escaped.replaceAll("`([^`]+)`", "<code>$1</code>");
        // Links: [text](url)
        escaped = escaped.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\">$1</a>");
        return escaped;
    }

    private String highlightJavaCode(String code) {
        if (code == null) {
            return "";
        }
        String escaped = escapeHtml(code);

        // Highlight annotations: @Identifier(...)
        escaped = escaped.replaceAll("(@\\w+)", "<span class=\"hl-annotation\">$1</span>");

        // Highlight Java Keywords
        String keywordsPattern = "\\b(public|private|protected|class|interface|record|enum|extends|implements|" +
                "return|if|else|switch|case|default|break|continue|throw|throws|try|catch|finally|" +
                "new|this|super|void|boolean|byte|short|int|long|float|double|char|String|" +
                "static|final|abstract|synchronized|volatile|transient|package|import)\\b";
        escaped = escaped.replaceAll(keywordsPattern, "<span class=\"hl-keyword\">$1</span>");

        // Highlight String literals
        escaped = escaped.replaceAll("&quot;(.*?)&quot;", "<span class=\"hl-string\">&quot;$1&quot;</span>");

        return escaped;
    }

    private String buildCss() {
        return """
                /* ==========================================================================
                   Blogify Design System: Primer (Default), Editorial, and Minimal Themes
                   ========================================================================== */

                /* 1. Base Variables: Primer Theme (Light) */
                :root, html[data-theme="primer"][data-mode="light"] {
                  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                  --font-serif: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
                  --font-mono: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
                  
                  --bg-primary: #ffffff;
                  --bg-secondary: #f6f8fa;
                  --bg-card: #ffffff;
                  --text-primary: #1f2328;
                  --text-secondary: #656d76;
                  --accent: #0969da;
                  --accent-hover: #0550ae;
                  --border: #d0d7de;
                  --tag-bg: #ddf4ff;
                  --tag-text: #0969da;
                  --code-bg: #f6f8fa;
                  --shadow: 0 1px 3px rgba(31,35,40,0.12), 0 1px 2px rgba(31,35,40,0.06);

                  --callout-problem-bg: #fff8c5;
                  --callout-problem-border: #d4a72c;
                  --callout-insight-bg: #ddf4ff;
                  --callout-insight-border: #54aeff;
                  --callout-takeaway-bg: #dafbe1;
                  --callout-takeaway-border: #4ac26b;
                  --callout-warning-bg: #ffebe9;
                  --callout-warning-border: #ff8182;
                }

                /* 2. Primer Theme (Dark) */
                html[data-theme="primer"][data-mode="dark"] {
                  --bg-primary: #0d1117;
                  --bg-secondary: #161b22;
                  --bg-card: #161b22;
                  --text-primary: #e6edf3;
                  --text-secondary: #848d97;
                  --accent: #2f81f7;
                  --accent-hover: #58a6ff;
                  --border: #30363d;
                  --tag-bg: rgba(56,139,253,0.15);
                  --tag-text: #58a6ff;
                  --code-bg: #161b22;
                  --shadow: 0 1px 3px rgba(0,0,0,0.4);

                  --callout-problem-bg: rgba(187,128,9,0.15);
                  --callout-problem-border: #bb8009;
                  --callout-insight-bg: rgba(56,139,253,0.15);
                  --callout-insight-border: #388bfd;
                  --callout-takeaway-bg: rgba(46,160,67,0.15);
                  --callout-takeaway-border: #2ea043;
                  --callout-warning-bg: rgba(248,81,73,0.15);
                  --callout-warning-border: #f85149;
                }

                /* 3. Editorial Theme (Light - Warm Paper & Serif) */
                html[data-theme="editorial"][data-mode="light"] {
                  --font-sans: "Charter", Georgia, "Times New Roman", serif;
                  --font-serif: "Charter", Georgia, "Times New Roman", serif;
                  --font-mono: ui-monospace, SFMono-Regular, monospace;

                  --bg-primary: #faf8f5;
                  --bg-secondary: #f2ece4;
                  --bg-card: #ffffff;
                  --text-primary: #24211e;
                  --text-secondary: #6b635b;
                  --accent: #b45309;
                  --accent-hover: #92400e;
                  --border: #e3dad0;
                  --tag-bg: #fef3c7;
                  --tag-text: #92400e;
                  --code-bg: #f5f0e8;
                  --shadow: 0 2px 6px rgba(44,33,20,0.08);

                  --callout-problem-bg: #fef3c7;
                  --callout-problem-border: #d97706;
                  --callout-insight-bg: #f0fdf4;
                  --callout-insight-border: #16a34a;
                  --callout-takeaway-bg: #f5f3ff;
                  --callout-takeaway-border: #7c3aed;
                  --callout-warning-bg: #fff1f2;
                  --callout-warning-border: #f43f5e;
                }

                /* 4. Editorial Theme (Dark - Deep Espresso) */
                html[data-theme="editorial"][data-mode="dark"] {
                  --font-sans: "Charter", Georgia, "Times New Roman", serif;
                  --font-serif: "Charter", Georgia, "Times New Roman", serif;
                  --font-mono: ui-monospace, SFMono-Regular, monospace;

                  --bg-primary: #191614;
                  --bg-secondary: #24201c;
                  --bg-card: #201c18;
                  --text-primary: #ede4dc;
                  --text-secondary: #9e9389;
                  --accent: #d97706;
                  --accent-hover: #f59e0b;
                  --border: #3b342e;
                  --tag-bg: rgba(217,119,6,0.18);
                  --tag-text: #fbbf24;
                  --code-bg: #24201c;
                  --shadow: 0 2px 8px rgba(0,0,0,0.5);

                  --callout-problem-bg: rgba(217,119,6,0.15);
                  --callout-problem-border: #d97706;
                  --callout-insight-bg: rgba(22,163,74,0.15);
                  --callout-insight-border: #16a34a;
                  --callout-takeaway-bg: rgba(124,58,237,0.15);
                  --callout-takeaway-border: #7c3aed;
                  --callout-warning-bg: rgba(244,63,94,0.15);
                  --callout-warning-border: #f43f5e;
                }

                /* 5. Minimal Theme (Light - Stark Hacker Monochrome) */
                html[data-theme="minimal"][data-mode="light"] {
                  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  --font-serif: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  --font-mono: "SF Mono", Monaco, Consolas, monospace;

                  --bg-primary: #ffffff;
                  --bg-secondary: #fafafa;
                  --bg-card: #ffffff;
                  --text-primary: #000000;
                  --text-secondary: #555555;
                  --accent: #000000;
                  --accent-hover: #333333;
                  --border: #000000;
                  --tag-bg: #eeeeee;
                  --tag-text: #000000;
                  --code-bg: #f9f9f9;
                  --shadow: none;

                  --callout-problem-bg: #fafafa;
                  --callout-problem-border: #000000;
                  --callout-insight-bg: #fafafa;
                  --callout-insight-border: #000000;
                  --callout-takeaway-bg: #fafafa;
                  --callout-takeaway-border: #000000;
                  --callout-warning-bg: #fafafa;
                  --callout-warning-border: #000000;
                }

                /* 6. Minimal Theme (Dark - Terminal Pitch Black) */
                html[data-theme="minimal"][data-mode="dark"] {
                  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  --font-serif: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  --font-mono: "SF Mono", Monaco, Consolas, monospace;

                  --bg-primary: #000000;
                  --bg-secondary: #0d0d0d;
                  --bg-card: #050505;
                  --text-primary: #ffffff;
                  --text-secondary: #999999;
                  --accent: #ffffff;
                  --accent-hover: #cccccc;
                  --border: #333333;
                  --tag-bg: #1a1a1a;
                  --tag-text: #ffffff;
                  --code-bg: #0d0d0d;
                  --shadow: none;

                  --callout-problem-bg: #0d0d0d;
                  --callout-problem-border: #666666;
                  --callout-insight-bg: #0d0d0d;
                  --callout-insight-border: #666666;
                  --callout-takeaway-bg: #0d0d0d;
                  --callout-takeaway-border: #666666;
                  --callout-warning-bg: #0d0d0d;
                  --callout-warning-border: #666666;
                }

                /* ==========================================================================
                   Layout & Components
                   ========================================================================== */
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: var(--font-sans);
                  background-color: var(--bg-primary);
                  color: var(--text-primary);
                  line-height: 1.65;
                  transition: background-color 0.2s ease, color 0.2s ease;
                }

                header {
                  background: var(--bg-secondary);
                  border-bottom: 1px solid var(--border);
                  padding: 1rem 1.5rem;
                }
                .header-container {
                  max-width: 920px;
                  margin: 0 auto;
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  gap: 1rem;
                }
                .brand-title {
                  font-size: 1.25rem;
                  font-weight: 700;
                  color: var(--text-primary);
                  text-decoration: none;
                  display: flex;
                  align-items: center;
                  gap: 0.5rem;
                }
                .badge-compile-time {
                  background: #238636;
                  color: #fff;
                  font-size: 0.65rem;
                  padding: 0.15rem 0.45rem;
                  border-radius: 12px;
                  font-weight: 700;
                  letter-spacing: 0.5px;
                }
                .header-actions {
                  display: flex;
                  align-items: center;
                  gap: 1.2rem;
                }
                .nav-links {
                  display: flex;
                  gap: 1rem;
                  align-items: center;
                }
                .nav-links a {
                  color: var(--text-secondary);
                  text-decoration: none;
                  font-weight: 500;
                  font-size: 0.9rem;
                }
                .nav-links a:hover { color: var(--accent); }
                .nav-ai-link {
                  background: var(--tag-bg);
                  color: var(--tag-text) !important;
                  padding: 0.15rem 0.5rem;
                  border-radius: 6px;
                  font-weight: 600 !important;
                  font-size: 0.8rem !important;
                }

                .theme-controls {
                  display: flex;
                  align-items: center;
                  gap: 0.4rem;
                }
                .theme-dropdown {
                  background: var(--bg-primary);
                  color: var(--text-primary);
                  border: 1px solid var(--border);
                  padding: 0.25rem 0.5rem;
                  border-radius: 6px;
                  font-size: 0.82rem;
                  font-family: inherit;
                  cursor: pointer;
                }
                .mode-btn {
                  background: var(--bg-primary);
                  color: var(--text-primary);
                  border: 1px solid var(--border);
                  padding: 0.25rem 0.5rem;
                  border-radius: 6px;
                  font-size: 0.9rem;
                  cursor: pointer;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                }

                main, article {
                  max-width: 920px;
                  margin: 2.5rem auto;
                  padding: 0 1.5rem;
                }

                .hero {
                  margin-bottom: 2.5rem;
                  border-bottom: 1px solid var(--border);
                  padding-bottom: 1.5rem;
                }
                .hero h1 { font-size: 2.3rem; margin-bottom: 0.5rem; letter-spacing: -0.5px; }
                .hero-subtitle { color: var(--text-secondary); font-size: 1.1rem; margin-bottom: 1rem; }
                .ai-badge-row { display: flex; gap: 0.6rem; align-items: center; }
                .ai-link-badge {
                  display: inline-block;
                  font-size: 0.8rem;
                  font-weight: 600;
                  color: var(--tag-text);
                  background: var(--tag-bg);
                  padding: 0.2rem 0.6rem;
                  border-radius: 6px;
                  text-decoration: none;
                  border: 1px solid var(--border);
                }

                .post-card {
                  background: var(--bg-card);
                  border: 1px solid var(--border);
                  border-radius: 8px;
                  padding: 1.5rem;
                  margin-bottom: 1.5rem;
                  box-shadow: var(--shadow);
                  transition: transform 0.15s ease, border-color 0.15s ease;
                }
                .post-card:hover {
                  transform: translateY(-2px);
                  border-color: var(--accent);
                }
                .post-card-retired {
                  opacity: 0.82;
                  border-left: 4px solid #cf222e;
                }
                .post-card-header {
                  display: flex;
                  justify-content: space-between;
                  align-items: baseline;
                  gap: 0.8rem;
                }
                .badge-retired {
                  font-size: 0.7rem;
                  font-weight: 700;
                  background: #cf222e;
                  color: #fff;
                  padding: 0.15rem 0.5rem;
                  border-radius: 10px;
                  letter-spacing: 0.5px;
                }
                .badge-home {
                  font-size: 0.7rem;
                  font-weight: 700;
                  background: #1f883d;
                  color: #fff;
                  padding: 0.15rem 0.5rem;
                  border-radius: 10px;
                  letter-spacing: 0.5px;
                }
                .post-title { font-size: 1.4rem; font-weight: 600; margin-bottom: 0.4rem; }
                .post-title a { color: var(--text-primary); text-decoration: none; }
                .post-title a:hover { color: var(--accent); }
                .post-meta {
                  font-size: 0.85rem;
                  color: var(--text-secondary);
                  margin-bottom: 0.75rem;
                  display: flex;
                  flex-wrap: wrap;
                  gap: 0.5rem;
                  align-items: center;
                }
                .post-desc { color: var(--text-secondary); font-size: 1rem; margin-bottom: 1rem; }
                .tag-list, .tags-container { display: flex; flex-wrap: wrap; gap: 0.45rem; }
                .tag {
                  background: var(--tag-bg);
                  color: var(--tag-text);
                  font-size: 0.75rem;
                  font-weight: 600;
                  padding: 0.18rem 0.55rem;
                  border-radius: 12px;
                }

                .archived-section {
                  margin-top: 3.5rem;
                  padding-top: 2rem;
                  border-top: 2px dashed var(--border);
                }
                .archived-heading {
                  font-size: 1.3rem;
                  color: var(--text-secondary);
                  margin-bottom: 0.4rem;
                }
                .archived-desc {
                  font-size: 0.9rem;
                  color: var(--text-secondary);
                  margin-bottom: 1.5rem;
                }

                .breadcrumb { font-size: 0.88rem; color: var(--text-secondary); margin-bottom: 1rem; }
                .breadcrumb a { color: var(--accent); text-decoration: none; font-weight: 500; }
                h1 { font-size: 2.3rem; line-height: 1.25; margin-bottom: 0.75rem; letter-spacing: -0.5px; }

                .post-meta-bar {
                  display: flex;
                  flex-wrap: wrap;
                  align-items: center;
                  gap: 1rem;
                  padding-bottom: 1.2rem;
                  border-bottom: 1px solid var(--border);
                  margin-bottom: 1.8rem;
                  color: var(--text-secondary);
                  font-size: 0.9rem;
                }
                .source-version-badge {
                  background: var(--bg-secondary);
                  border: 1px solid var(--border);
                  padding: 0.2rem 0.6rem;
                  border-radius: 6px;
                  font-size: 0.82rem;
                }
                .visitor-counter-badge {
                  display: inline-flex;
                  align-items: center;
                  gap: 0.35rem;
                  background: var(--bg-secondary);
                  border: 1px solid var(--border);
                  padding: 0.2rem 0.6rem;
                  border-radius: 6px;
                  font-size: 0.82rem;
                  color: var(--text-secondary);
                }
                .commit-link { color: var(--accent); text-decoration: none; font-weight: 500; }
                .commit-link:hover { text-decoration: underline; }

                /* Story-First Callouts */
                .callout {
                  border-radius: 8px;
                  padding: 1.1rem 1.4rem;
                  margin: 1.6rem 0;
                  border-left: 5px solid;
                }
                .callout-header {
                  font-weight: 700;
                  font-size: 1rem;
                  margin-bottom: 0.5rem;
                  display: flex;
                  align-items: center;
                  gap: 0.4rem;
                }
                .callout-body { font-size: 1.02rem; line-height: 1.65; }
                .callout-body p { margin: 0 0 0.6rem 0; }
                .callout-body p:last-child { margin-bottom: 0; }
                .callout-body ul { margin: 0.5rem 0 0.5rem 1.4rem; padding: 0; }
                .callout-body li { margin-bottom: 0.4rem; }
                .callout-body li:last-child { margin-bottom: 0; }
                .callout-problem {
                  background: var(--callout-problem-bg);
                  border-left-color: var(--callout-problem-border);
                }
                .callout-insight {
                  background: var(--callout-insight-bg);
                  border-left-color: var(--callout-insight-border);
                }
                .callout-takeaways {
                  background: var(--callout-takeaway-bg);
                  border-left-color: var(--callout-takeaway-border);
                }
                .takeaways-list { margin-left: 1.3rem; margin-top: 0.4rem; }
                .takeaways-list li { margin-bottom: 0.35rem; }
                .callout-warning {
                  background: var(--callout-warning-bg);
                  border-left-color: var(--callout-warning-border);
                }
                .callout-retired { margin-bottom: 2rem; }

                /* Architecture Diagram Card */
                .diagram-card {
                  background: var(--bg-card);
                  border: 1px solid var(--border);
                  border-radius: 8px;
                  padding: 1.4rem;
                  margin: 2rem 0;
                  box-shadow: var(--shadow);
                  text-align: center;
                }
                .diagram-header {
                  font-weight: 700;
                  font-size: 0.92rem;
                  color: var(--text-secondary);
                  margin-bottom: 1.2rem;
                  text-align: left;
                  text-transform: uppercase;
                  letter-spacing: 0.5px;
                }
                .mermaid {
                  display: flex;
                  justify-content: center;
                }

                .intro-box {
                  background: var(--bg-secondary);
                  border-left: 4px solid var(--accent);
                  padding: 1rem 1.25rem;
                  border-radius: 0 6px 6px 0;
                  margin-bottom: 2rem;
                  font-size: 1.05rem;
                }
                h2 { font-size: 1.55rem; margin-top: 2.2rem; margin-bottom: 0.8rem; padding-bottom: 0.35rem; border-bottom: 1px solid var(--border); }
                h3 { font-size: 1.25rem; margin-top: 1.6rem; margin-bottom: 0.6rem; }
                p { margin-bottom: 1.1rem; }
                .md-li { margin-left: 1.4rem; margin-bottom: 0.4rem; }
                .md-quote {
                  border-left: 4px solid var(--border);
                  padding-left: 1rem;
                  color: var(--text-secondary);
                  font-style: italic;
                  margin: 1.2rem 0;
                }
                .md-ul { margin: 0.6rem 0 1rem 1.4rem; padding: 0; }
                .md-li { margin-bottom: 0.35rem; line-height: 1.6; }
                .img-wrapper { text-align: center; margin: 2rem 0; }
                .md-img {
                  max-width: 100%;
                  height: auto;
                  border-radius: 8px;
                  border: 1px solid var(--border);
                  box-shadow: var(--shadow);
                }

                /* Auto-playing Carousel */
                .carousel-container {
                  position: relative;
                  overflow: hidden;
                  border-radius: 10px;
                  border: 1px solid var(--border);
                  box-shadow: var(--shadow);
                  margin: 2.2rem 0;
                  background: var(--bg-secondary);
                }
                .carousel-track {
                  position: relative;
                  width: 100%;
                  min-height: 200px;
                }
                .carousel-slide {
                  display: none;
                  opacity: 0;
                  transition: opacity 0.5s ease-in-out;
                  text-align: center;
                }
                .carousel-slide.active {
                  display: block;
                  opacity: 1;
                }
                .carousel-img {
                  width: 100%;
                  height: auto;
                  max-height: 540px;
                  object-fit: cover;
                  display: block;
                }
                .carousel-caption {
                  position: absolute;
                  bottom: 0;
                  left: 0;
                  right: 0;
                  background: linear-gradient(transparent, rgba(0, 0, 0, 0.88));
                  color: #ffffff;
                  padding: 1.5rem 1.25rem 2.2rem 1.25rem;
                  font-weight: 600;
                  font-size: 0.98rem;
                  text-align: center;
                  letter-spacing: 0.3px;
                  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.9);
                }
                .carousel-prev,
                .carousel-next {
                  position: absolute;
                  top: 50%;
                  transform: translateY(-50%);
                  background: rgba(0, 0, 0, 0.45);
                  color: #ffffff;
                  border: none;
                  width: 42px;
                  height: 42px;
                  border-radius: 50%;
                  cursor: pointer;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  font-size: 1.3rem;
                  transition: background 0.2s, transform 0.2s;
                  z-index: 10;
                  user-select: none;
                }
                .carousel-prev:hover,
                .carousel-next:hover {
                  background: rgba(0, 0, 0, 0.85);
                  transform: translateY(-50%) scale(1.1);
                }
                .carousel-prev { left: 14px; }
                .carousel-next { right: 14px; }
                .carousel-dots {
                  position: absolute;
                  bottom: 10px;
                  left: 0;
                  right: 0;
                  display: flex;
                  justify-content: center;
                  gap: 9px;
                  z-index: 11;
                }
                .carousel-dot {
                  width: 10px;
                  height: 10px;
                  border-radius: 50%;
                  background: rgba(255, 255, 255, 0.45);
                  cursor: pointer;
                  transition: all 0.25s ease;
                }
                .carousel-dot.active {
                  background: #ffffff;
                  transform: scale(1.35);
                  box-shadow: 0 0 5px rgba(255, 255, 255, 0.9);
                }

                /* Code wrapper and collapsible drawers */
                .code-wrapper {
                  margin: 1.4rem 0 2rem 0;
                  border: 1px solid var(--border);
                  border-radius: 8px;
                  overflow: hidden;
                  background: var(--code-bg);
                  box-shadow: var(--shadow);
                }
                .code-header {
                  background: var(--bg-secondary);
                  padding: 0.55rem 1rem;
                  border-bottom: 1px solid var(--border);
                  font-size: 0.82rem;
                  font-weight: 600;
                  display: flex;
                  justify-content: space-between;
                  color: var(--text-secondary);
                }
                pre {
                  padding: 1.1rem;
                  overflow-x: auto;
                  font-family: var(--font-mono);
                  font-size: 0.88rem;
                  line-height: 1.55;
                }
                .code-drawer {
                  border-top: 1px solid var(--border);
                  background: var(--bg-secondary);
                }
                .code-drawer-summary {
                  padding: 0.6rem 1rem;
                  font-size: 0.83rem;
                  font-weight: 600;
                  color: var(--accent);
                  cursor: pointer;
                  user-select: none;
                }
                .code-drawer-summary:hover {
                  text-decoration: underline;
                }
                .drawer-code {
                  border-top: 1px solid var(--border);
                  background: var(--code-bg);
                }

                /* Code syntax highlighting */
                .hl-keyword { color: #cf222e; font-weight: 600; }
                .hl-string { color: #0a3069; }
                .hl-annotation { color: #8250df; }
                html[data-mode="dark"] .hl-keyword { color: #ff7b72; }
                html[data-mode="dark"] .hl-string { color: #a5d6ff; }
                html[data-mode="dark"] .hl-annotation { color: #d2a8ff; }

                /* Mini cards in homepage footer section */
                .home-more-articles {
                  margin-top: 3.5rem;
                  padding-top: 2rem;
                  border-top: 1px solid var(--border);
                }
                .more-articles-header {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  margin-bottom: 1.5rem;
                }
                .more-articles-header h2 {
                  font-size: 1.35rem;
                  margin: 0;
                }
                .view-all-link {
                  font-size: 0.95rem;
                  font-weight: 600;
                  color: var(--accent);
                  text-decoration: none;
                }
                .view-all-link:hover {
                  text-decoration: underline;
                }
                .more-articles-grid {
                  display: grid;
                  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
                  gap: 1.25rem;
                }
                .mini-card {
                  background: var(--bg-card);
                  border: 1px solid var(--border);
                  border-radius: 8px;
                  padding: 1.1rem 1.25rem;
                  transition: transform 0.15s ease, box-shadow 0.15s ease;
                }
                .mini-card:hover {
                  transform: translateY(-2px);
                  box-shadow: var(--shadow);
                }
                .mini-card h3 {
                  font-size: 1.05rem;
                  margin: 0 0 0.5rem 0;
                  font-weight: 600;
                }
                .mini-card h3 a {
                  color: var(--text-primary);
                  text-decoration: none;
                }
                .mini-card h3 a:hover {
                  color: var(--accent);
                }
                .mini-card-desc {
                  font-size: 0.88rem;
                  color: var(--text-secondary);
                  line-height: 1.4;
                  margin: 0 0 0.6rem 0;
                }
                .mini-card-meta {
                  font-size: 0.8rem;
                  color: var(--text-secondary);
                }

                footer {
                  border-top: 1px solid var(--border);
                  text-align: center;
                  padding: 2.5rem 1.5rem;
                  color: var(--text-secondary);
                  font-size: 0.85rem;
                  margin-top: 4rem;
                }
                .footer-container { max-width: 920px; margin: 0 auto; }
                .footer-ai-note { margin-top: 0.5rem; font-size: 0.8rem; }
                .footer-ai-note a { color: var(--accent); text-decoration: none; }
                """;
    }

    private String renderCalloutBody(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] lines = text.split("\r?\n");
        StringBuilder sb = new StringBuilder();
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    sb.append("        <ul>\n");
                    inList = true;
                }
                sb.append("          <li>").append(escapeHtml(trimmed.substring(2).trim())).append("</li>\n");
            } else {
                if (inList) {
                    sb.append("        </ul>\n");
                    inList = false;
                }
                sb.append("        <p>").append(escapeHtml(trimmed)).append("</p>\n");
            }
        }
        if (inList) {
            sb.append("        </ul>\n");
        }
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public String buildPortfolioBlogsJson(List<BlogPostModel> posts, String siteUrl, BlogPostModel homePost) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < posts.size(); i++) {
            BlogPostModel p = posts.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": ").append(escapeJsonString(p.getSlug())).append(",\n");
            sb.append("    \"title\": ").append(escapeJsonString(p.getTitle())).append(",\n");
            sb.append("    \"description\": ").append(escapeJsonString(p.getDescription())).append(",\n");
            sb.append("    \"date\": ").append(escapeJsonString(p.getDate())).append(",\n");
            sb.append("    \"category\": ").append(escapeJsonString(p.getCategory())).append(",\n");
            sb.append("    \"link\": ").append(escapeJsonString(resolvePublicPostUrl(p, siteUrl, homePost))).append(",\n");
            sb.append("    \"tags\": [");
            List<String> tags = new ArrayList<>(p.getTags());
            for (int t = 0; t < tags.size(); t++) {
                sb.append(escapeJsonString(tags.get(t)));
                if (t < tags.size() - 1) {
                    sb.append(", ");
                }
            }
            sb.append("]\n");
            sb.append("  }");
            if (i < posts.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    public static String resolvePublicPostUrl(BlogPostModel post, String siteUrl, BlogPostModel homePost) {
        String base = siteUrl;
        if (base == null || base.isBlank()) {
            String repo = post.getRepoUrl();
            if (repo != null && repo.contains("github.com/")) {
                String path = repo.substring(repo.indexOf("github.com/") + "github.com/".length());
                if (path.endsWith(".git")) {
                    path = path.substring(0, path.length() - 4);
                }
                String[] parts = path.split("/");
                if (parts.length >= 2) {
                    base = "https://" + parts[0].toLowerCase() + ".github.io/" + parts[1];
                }
            }
        }
        if (base == null || base.isBlank()) {
            base = "https://krushnatkhawale.github.io/blogify";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (post == homePost || post.isHome() || "index".equalsIgnoreCase(post.getSlug())) {
            return base + "/";
        } else {
            return base + "/" + post.getSlug() + ".html";
        }
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String escapeJsonString(String text) {
        return "\"" + escapeJson(text) + "\"";
    }

    private String buildGoogleAnalyticsScript(String gaId) {
        if (gaId == null || gaId.isBlank()) {
            return "";
        }
        String cleanId = escapeHtml(gaId.trim());
        return "  <!-- Google tag (gtag.js) -->\n"
                + "  <script async src=\"https://www.googletagmanager.com/gtag/js?id=" + cleanId + "\"></script>\n"
                + "  <script>\n"
                + "    window.dataLayer = window.dataLayer || [];\n"
                + "    function gtag(){dataLayer.push(arguments);}\n"
                + "    gtag('js', new Date());\n"
                + "    gtag('config', '" + cleanId + "', {\n"
                + "      page_path: window.location.pathname,\n"
                + "      page_title: document.title\n"
                + "    });\n"
                + "  </script>\n";
    }

    private String buildVisitorCounterScript(String namespace, String key) {
        if (namespace == null || namespace.isBlank() || key == null || key.isBlank()) {
            return "";
        }
        String cleanNs = escapeJson(namespace.trim().toLowerCase());
        String cleanKey = escapeJson(key.trim().toLowerCase());
        return "  <script>\n"
                + "    (function() {\n"
                + "      var ns = \"" + cleanNs + "\";\n"
                + "      var key = \"" + cleanKey + "\";\n"
                + "      var badge = document.getElementById('blogify-views-badge');\n"
                + "      var countEl = document.getElementById('blogify-views-count');\n"
                + "      if (!badge || !countEl) return;\n"
                + "      var fullKey = (ns ? ns + '_' : '') + key;\n"
                + "      var isLocal = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';\n"
                + "      var endpoint = isLocal ? 'get' : 'hit';\n"
                + "      fetch('https://countapi.mileshilliard.com/api/v1/' + endpoint + '/' + encodeURIComponent(fullKey))\n"
                + "        .then(function(res) { return res.ok ? res.json() : null; })\n"
                + "        .then(function(data) {\n"
                + "          var count = data && (typeof data.value !== 'undefined' ? data.value : data.count);\n"
                + "          if (typeof count !== 'undefined') {\n"
                + "            countEl.textContent = Number(count).toLocaleString();\n"
                + "            badge.style.display = 'inline-flex';\n"
                + "          }\n"
                + "        })\n"
                + "        .catch(function(err) {\n"
                + "          console.debug('Visitor counter unavailable', err);\n"
                + "        });\n"
                + "    })();\n"
                + "  </script>\n";
    }

    public static String computeCounterNamespace(String customNamespace, String siteTitle, String repoUrl) {
        if (customNamespace != null && !customNamespace.isBlank()) {
            return sanitizeNamespace(customNamespace);
        }
        if (repoUrl != null && repoUrl.contains("github.com/")) {
            String path = repoUrl.substring(repoUrl.indexOf("github.com/") + "github.com/".length());
            if (path.endsWith(".git")) {
                path = path.substring(0, path.length() - 4);
            }
            String sanitized = sanitizeNamespace(path.replace("/", "-"));
            if (!sanitized.isBlank()) {
                return sanitized;
            }
        }
        if (siteTitle != null && !siteTitle.isBlank()) {
            String sanitized = sanitizeNamespace(siteTitle);
            if (!sanitized.isBlank()) {
                return sanitized;
            }
        }
        return "blogify";
    }

    private static String sanitizeNamespace(String str) {
        if (str == null) return "";
        return str.toLowerCase().replaceAll("[^a-z0-9_-]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
    }
}

