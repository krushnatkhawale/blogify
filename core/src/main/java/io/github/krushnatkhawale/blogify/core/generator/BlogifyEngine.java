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
        BlogFormat format = BlogFormat.fromString(formatStr);
        String selectedTheme = (theme != null && !theme.isBlank()) ? theme : "primer";
        List<BlogPostModel> allPosts = new ArrayList<>();

        for (File dir : sourceDirs) {
            if (dir != null && dir.exists()) {
                allPosts.addAll(scanner.scan(dir));
            }
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<Path> writtenFiles = new ArrayList<>();

        // Copy static image assets from sourceDirs (e.g. leaf bundles in blogs/)
        for (File dir : sourceDirs) {
            if (dir != null && dir.exists()) {
                writtenFiles.addAll(copyStaticAssets(dir.toPath(), outputDir.toPath()));
                if (format == BlogFormat.GITHUB_PAGES) {
                    writtenFiles.addAll(copyStaticAssets(dir.toPath(), new File(outputDir, "preview").toPath()));
                }
            }
        }

        switch (format) {
            case GITHUB_PAGES -> {
                // Generate Jekyll structure for GitHub Pages
                writtenFiles.addAll(jekyllGenerator.generate(outputDir, siteTitle, allPosts, includeDrafts));

                // Always generate an HTML preview folder inside outputDir/preview for zero-setup local visualization
                File previewDir = new File(outputDir, "preview");
                htmlGenerator.generate(previewDir, siteTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace);
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
                // Generate standalone static HTML, CSS, llms.txt directly into outputDir
                writtenFiles.addAll(htmlGenerator.generate(outputDir, siteTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace));
                this.lastGeneratedPortfolioJson = htmlGenerator.getLastGeneratedPortfolioJson();
            }
            case MARKDOWN -> {
                for (BlogPostModel post : allPosts) {
                    String markdown = markdownGenerator.generate(post);
                    Path targetFile = outputDir.toPath().resolve(post.getSlug() + ".md");
                    Files.writeString(targetFile, markdown, StandardCharsets.UTF_8);
                    writtenFiles.add(targetFile);
                }

                if (generateIndex) {
                    String indexMarkdown = indexGenerator.generateIndex(siteTitle, allPosts);
                    Path indexFile = outputDir.toPath().resolve("index.md");
                    Files.writeString(indexFile, indexMarkdown, StandardCharsets.UTF_8);
                    writtenFiles.add(indexFile);
                }

                File previewDir = new File(outputDir, "preview");
                htmlGenerator.generate(previewDir, siteTitle, allPosts, selectedTheme, includeDrafts, blogHome, siteUrl, googleAnalyticsId, enableVisitorCounter, counterNamespace);
                this.lastGeneratedPortfolioJson = htmlGenerator.getLastGeneratedPortfolioJson();
            }
        }

        return writtenFiles;
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
