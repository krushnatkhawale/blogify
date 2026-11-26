package io.github.krushnatkhawale.blogify.core.parser;

import io.github.krushnatkhawale.blogify.core.model.BlogPostModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Traverses directories to locate and parse Java source files and Markdown blog bundles.
 */
public class BlogSourceScanner {

    private final JavaSourceBlogParser javaParser = new JavaSourceBlogParser();
    private final MarkdownBlogParser markdownParser = new MarkdownBlogParser();
    private BlogifyMetadata lastScannedMetadata = new BlogifyMetadata();

    public BlogifyMetadata getLastScannedMetadata() {
        return lastScannedMetadata;
    }

    public List<BlogPostModel> scan(File rootDir) throws IOException {
        if (rootDir == null || !rootDir.exists()) {
            return Collections.emptyList();
        }
        return scan(rootDir.toPath());
    }

    public List<BlogPostModel> scan(Path rootPath) throws IOException {
        if (rootPath == null || !Files.exists(rootPath)) {
            return Collections.emptyList();
        }

        // Scan for blogify.info or plugin.info
        BlogifyMetadata dirMeta = BlogifyMetadata.findInDirectory(rootPath.toFile());
        if (!dirMeta.isEmpty()) {
            this.lastScannedMetadata = dirMeta;
        }

        List<BlogPostModel> posts = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(rootPath)) {
            stream.filter(Files::isRegularFile)
                  .forEach(p -> {
                      String pathStr = p.toString();
                      try {
                          if (pathStr.endsWith(".java")) {
                              posts.addAll(javaParser.parse(p));
                          } else if (pathStr.endsWith(".md") && !p.getFileName().toString().equalsIgnoreCase("README.md")) {
                              BlogPostModel mdPost = markdownParser.parse(p.toFile());
                              if (mdPost != null) {
                                  posts.add(mdPost);
                              }
                          }
                      } catch (Exception e) {
                          System.err.println("[Blogify] Warning: failed to parse " + p + ": " + e.getMessage());
                      }
                  });
        }

        // Apply metadata defaults to posts if not already populated
        if (!lastScannedMetadata.isEmpty()) {
            for (BlogPostModel post : posts) {
                if (post.getVersion().isBlank() && !lastScannedMetadata.getVersion().isBlank()) {
                    post.setVersion(lastScannedMetadata.getVersion());
                }
                if (post.getAuthor().isBlank() && !lastScannedMetadata.getAuthor().isBlank()) {
                    post.setAuthor(lastScannedMetadata.getAuthor());
                }
                if (post.getDate().isBlank() && !lastScannedMetadata.getDate().isBlank()) {
                    post.setDate(lastScannedMetadata.getDate());
                }
                if (post.getCategory().equals("Engineering") && !lastScannedMetadata.getCategory().isBlank()) {
                    post.setCategory(lastScannedMetadata.getCategory());
                }
                if (post.getTags().isEmpty() && !lastScannedMetadata.getTags().isEmpty()) {
                    post.addTags(lastScannedMetadata.getTags());
                }
                if (post.getSiteTitle().isBlank() && !lastScannedMetadata.getSiteTitle().isBlank()) {
                    post.setSiteTitle(lastScannedMetadata.getSiteTitle());
                }
            }
        }

        Collections.sort(posts);
        return posts;
    }
}
