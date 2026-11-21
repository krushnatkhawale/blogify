package io.github.blogify.core.generator;

import io.github.blogify.core.model.BlogPostModel;
import io.github.blogify.core.parser.BlogSourceScanner;

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

    public BlogifyEngine() {
        this.scanner = new BlogSourceScanner();
        this.markdownGenerator = new MarkdownGenerator();
        this.indexGenerator = new BlogSiteIndexGenerator();
    }

    public List<Path> generate(List<File> sourceDirs, File outputDir, String siteTitle, boolean generateIndex) throws IOException {
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

        return writtenFiles;
    }
}
