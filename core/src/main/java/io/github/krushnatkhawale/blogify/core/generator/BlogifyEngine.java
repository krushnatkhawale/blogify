package io.github.krushnatkhawale.blogify.core.generator;

import io.github.krushnatkhawale.blogify.core.model.BlogPostModel;
import io.github.krushnatkhawale.blogify.core.parser.BlogSourceScanner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Main engine executing the scanning and static blog post generation.
 */
public class BlogifyEngine {

    private final BlogSourceScanner scanner;
    private final MarkdownGenerator markdownGenerator;
    private final BlogSiteIndexGenerator indexGenerator;
    private final JekyllGenerator jekyllGenerator;
    private final HtmlGenerator htmlGenerator;

    public BlogifyEngine() {
        this.scanner = new BlogSourceScanner();
        this.markdownGenerator = new MarkdownGenerator();
        this.indexGenerator = new BlogSiteIndexGenerator();
        this.jekyllGenerator = new JekyllGenerator();
        this.htmlGenerator = new HtmlGenerator();
    }

    private String lastGeneratedPortfolioJson = "";

    public String getLastGeneratedPortfolioJson() {
        return lastGeneratedPortfolioJson;
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex) throws IOException {
        return generate(sourceDirs, outputDir, siteTitle, generateIndex, BlogFormat.GITHUB_PAGES.getValue(), "primer");
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex, String formatStr) throws IOException {
        return generate(sourceDirs, outputDir, siteTitle, generateIndex, formatStr, "primer");
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex, String formatStr, String theme) throws IOException {
        return generate(sourceDirs, outputDir, siteTitle, generateIndex, formatStr, theme, false);
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex, String formatStr, String theme, boolean includeDrafts) throws IOException {
        return generate(sourceDirs, outputDir, siteTitle, generateIndex, formatStr, theme, includeDrafts, null, null);
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex, String formatStr, String theme, boolean includeDrafts, String blogHome, String siteUrl) throws IOException {
        return generate(sourceDirs, outputDir, siteTitle, generateIndex, formatStr, theme, includeDrafts, blogHome, siteUrl, null, false, null);
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex, String formatStr, String theme, boolean includeDrafts, String blogHome, String siteUrl, String googleAnalyticsId, boolean enableVisitorCounter, String counterNamespace) throws IOException {
        return generate(sourceDirs, outputDir, siteTitle, generateIndex, formatStr, theme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, null);
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex, String formatStr, String theme, boolean includeDrafts, String blogHome, String siteUrl, String googleAnalyticsId, boolean enableVisitorCounter, String counterNamespace, String version) throws IOException {
        BlogFormat format = BlogFormat.fromString(formatStr);
        String selectedTheme = (theme != null && !theme.isBlank()) ? theme : "primer";
        List<BlogPostModel> allPosts = new ArrayList<>();

        for (File dir : sourceDirs) {
            if (dir != null && dir.exists()) {
                allPosts.addAll(scanner.scan(dir));
            }
        }

        io.github.krushnatkhawale.blogify.core.parser.BlogifyMetadata meta = scanner.getLastScannedMetadata();

        // 1. Resolve site title
        String resolvedTitle = siteTitle;
        if (!meta.getSiteTitle().isBlank()) {
            resolvedTitle = meta.getSiteTitle();
        } else {
            for (BlogPostModel post : allPosts) {
                if (!post.getSiteTitle().isBlank()) {
                    resolvedTitle = post.getSiteTitle();
                    break;
                }
            }
        }
        if (isDefaultOrGenericTitle(resolvedTitle) && !allPosts.isEmpty()) {
            BlogPostModel primary = allPosts.get(0);
            if (!primary.getTitle().isBlank()) {
                resolvedTitle = primary.getTitle();
            }
        }
        if (resolvedTitle == null || resolvedTitle.isBlank()) {
            resolvedTitle = (siteTitle != null && !siteTitle.isBlank()) ? siteTitle : "Project Blog";
        }

        // 2. Resolve version
        String resolvedVersion = (version != null && !version.isBlank() && !"unspecified".equalsIgnoreCase(version)) ? version : "";
        if (resolvedVersion.isBlank() && !meta.getVersion().isBlank()) {
            resolvedVersion = meta.getVersion();
        }
        if (resolvedVersion.isBlank()) {
            for (BlogPostModel post : allPosts) {
                if (!post.getVersion().isBlank()) {
                    resolvedVersion = post.getVersion();
                    break;
                }
            }
        }
        if ((resolvedVersion.startsWith("v") || resolvedVersion.startsWith("V")) && resolvedVersion.length() > 1 && Character.isDigit(resolvedVersion.charAt(1))) {
            resolvedVersion = resolvedVersion.substring(1);
        }

        // 3. Resolve slug
        String slug = meta.getSlug();
        if (slug.isBlank()) {
            slug = slugify(resolvedTitle);
        }

        File versionedDir = !resolvedVersion.isBlank() ? new File(outputDir, slug + "/" + resolvedVersion) : new File(outputDir, slug);
        versionedDir.mkdirs();

        List<Path> writtenFiles = new ArrayList<>();

        // Copy static image assets into outputDir and versioned directory
        for (File dir : sourceDirs) {
            if (dir != null && dir.exists()) {
                writtenFiles.addAll(copyStaticAssets(dir.toPath(), outputDir.toPath()));
                writtenFiles.addAll(copyStaticAssets(dir.toPath(), versionedDir.toPath()));
                if (format == BlogFormat.GITHUB_PAGES) {
                    writtenFiles.addAll(copyStaticAssets(dir.toPath(), new File(outputDir, "preview").toPath()));
                }
            }
        }

        switch (format) {
            case GITHUB_PAGES -> {
                // Generate Jekyll structure for GitHub Pages at output root
                writtenFiles.addAll(jekyllGenerator.generate(outputDir, resolvedTitle, allPosts, includeDrafts));

                // Generate versioned static HTML into versionedDir (<slug>/<version>)
                writtenFiles.addAll(htmlGenerator.generate(versionedDir, resolvedTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, slug, resolvedVersion));

                // Always generate an HTML preview folder inside outputDir/preview for zero-setup local visualization
                File previewDir = new File(outputDir, "preview");
                htmlGenerator.generate(previewDir, resolvedTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, slug, resolvedVersion);
                File previewVersionedDir = new File(previewDir, slug + "/" + resolvedVersion);
                htmlGenerator.generate(previewVersionedDir, resolvedTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, slug, resolvedVersion);
                for (File dir : sourceDirs) {
                    if (dir != null && dir.exists()) {
                        copyStaticAssets(dir.toPath(), previewVersionedDir.toPath());
                    }
                }
                this.lastGeneratedPortfolioJson = htmlGenerator.getLastGeneratedPortfolioJson();

                // Also generate AI discoverability files at output root
                Path llmsFile = outputDir.toPath().resolve("llms.txt");
                Files.copy(previewDir.toPath().resolve("llms.txt"), llmsFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                writtenFiles.add(llmsFile);

                Path llmsFullFile = outputDir.toPath().resolve("llms-full.txt");
                Files.copy(previewDir.toPath().resolve("llms-full.txt"), llmsFullFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                writtenFiles.add(llmsFullFile);

                Path robotsFile = outputDir.toPath().resolve("robots.txt");
                Files.copy(previewDir.toPath().resolve("robots.txt"), robotsFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                writtenFiles.add(robotsFile);

                Path blogsJsonFile = outputDir.toPath().resolve("blogs.json");
                Files.copy(previewDir.toPath().resolve("blogs.json"), blogsJsonFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                writtenFiles.add(blogsJsonFile);
            }
            case HTML -> {
                // Generate standalone static HTML at root and versionedDir
                writtenFiles.addAll(htmlGenerator.generate(outputDir, resolvedTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, slug, resolvedVersion));
                writtenFiles.addAll(htmlGenerator.generate(versionedDir, resolvedTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, slug, resolvedVersion));
                this.lastGeneratedPortfolioJson = htmlGenerator.getLastGeneratedPortfolioJson();
            }
            case MARKDOWN -> {
                for (BlogPostModel post : allPosts) {
                    String markdown = markdownGenerator.generate(post);
                    Path targetFile = outputDir.toPath().resolve(post.getSlug() + ".md");
                    Files.writeString(targetFile, markdown, StandardCharsets.UTF_8);
                    writtenFiles.add(targetFile);

                    Path vTargetFile = versionedDir.toPath().resolve(post.getSlug() + ".md");
                    Files.writeString(vTargetFile, markdown, StandardCharsets.UTF_8);
                    writtenFiles.add(vTargetFile);
                }

                if (generateIndex) {
                    String indexMarkdown = indexGenerator.generateIndex(resolvedTitle, allPosts);
                    Path indexFile = outputDir.toPath().resolve("index.md");
                    Files.writeString(indexFile, indexMarkdown, StandardCharsets.UTF_8);
                    writtenFiles.add(indexFile);

                    Path vIndexFile = versionedDir.toPath().resolve("index.md");
                    Files.writeString(vIndexFile, indexMarkdown, StandardCharsets.UTF_8);
                    writtenFiles.add(vIndexFile);
                }

                File previewDir = new File(outputDir, "preview");
                htmlGenerator.generate(previewDir, resolvedTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace, slug, resolvedVersion);
                this.lastGeneratedPortfolioJson = htmlGenerator.getLastGeneratedPortfolioJson();
            }
        }

        return writtenFiles;
    }

    public static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "blog";
        }
        String slug = text.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return slug.isBlank() ? "blog" : slug;
    }

    public static boolean isDefaultOrGenericTitle(String title) {
        if (title == null || title.isBlank()) {
            return true;
        }
        String t = title.trim();
        return t.equalsIgnoreCase("Project Blog") || t.equalsIgnoreCase("Blog");
    }

    private String buildRedirectHtml(String title, String version, String targetRelativeUrl) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta http-equiv=\"refresh\" content=\"0; url=" + targetRelativeUrl + "\">\n" +
                "  <title>" + escapeHtml(title) + "</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <p>Redirecting to <a href=\"" + targetRelativeUrl + "\">" + escapeHtml(title) + " (v" + escapeHtml(version) + ")</a>...</p>\n" +
                "</body>\n" +
                "</html>";
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

    private List<Path> copyStaticAssets(Path sourceDir, Path targetDir) {
        List<Path> copied = new ArrayList<>();
        if (!Files.exists(sourceDir)) {
            return copied;
        }
        try (var stream = Files.walk(sourceDir)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> isImageOrAsset(p.getFileName().toString()))
                  .forEach(p -> {
                      try {
                          Files.createDirectories(targetDir);
                          Path dest = targetDir.resolve(p.getFileName().toString());
                          Files.copy(p, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                          copied.add(dest);
                      } catch (Exception ignored) {
                      }
                  });
        } catch (Exception ignored) {
        }
        return copied;
    }

    private boolean isImageOrAsset(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".svg") || lower.endsWith(".gif") || lower.endsWith(".webp") ||
                lower.endsWith(".ico");
    }
}
