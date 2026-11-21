package io.github.blogify.core.generator;

import io.github.blogify.core.model.BlogCodeBlockModel;
import io.github.blogify.core.model.BlogPostModel;
import io.github.blogify.core.model.BlogSectionModel;

public class MarkdownGenerator {

    public String generate(BlogPostModel post) {
        StringBuilder sb = new StringBuilder();

        // 1. YAML Frontmatter
        sb.append("---\n");
        sb.append("title: \"").append(escapeYaml(post.getTitle())).append("\"\n");
        sb.append("slug: \"").append(escapeYaml(post.getSlug())).append("\"\n");
        if (!post.getAuthor().isBlank()) {
            sb.append("author: \"").append(escapeYaml(post.getAuthor())).append("\"\n");
        }
        if (!post.getDate().isBlank()) {
            sb.append("date: \"").append(escapeYaml(post.getDate())).append("\"\n");
        }
        if (!post.getDescription().isBlank()) {
            sb.append("description: \"").append(escapeYaml(post.getDescription())).append("\"\n");
        }
        sb.append("draft: ").append(post.isDraft()).append("\n");
        if (!post.getTags().isEmpty()) {
            sb.append("tags:\n");
            for (String tag : post.getTags()) {
                sb.append("  - \"").append(escapeYaml(tag)).append("\"\n");
            }
        }
        sb.append("---\n\n");

        // 2. Post Title
        sb.append("# ").append(post.getTitle()).append("\n\n");

        // Metadata badges / subtitle
        if (!post.getAuthor().isBlank() || !post.getDate().isBlank()) {
            sb.append("> *");
            if (!post.getAuthor().isBlank()) {
                sb.append("By ").append(post.getAuthor());
            }
            if (!post.getAuthor().isBlank() && !post.getDate().isBlank()) {
                sb.append(" | ");
            }
            if (!post.getDate().isBlank()) {
                sb.append("Published on ").append(post.getDate());
            }
            sb.append("*\n\n");
        }

        // 3. Intro / Javadoc content
        if (!post.getIntroContent().isBlank()) {
            sb.append(post.getIntroContent()).append("\n\n");
        }

        // 4. Standalone code blocks
        for (BlogCodeBlockModel cb : post.getStandaloneCodeBlocks()) {
            appendCodeBlock(sb, cb);
        }

        // 5. Sections
        for (BlogSectionModel section : post.getSections()) {
            sb.append("## ").append(section.getHeading()).append("\n\n");
            if (!section.getContent().isBlank()) {
                sb.append(section.getContent()).append("\n\n");
            }
            for (BlogCodeBlockModel cb : section.getCodeBlocks()) {
                appendCodeBlock(sb, cb);
            }
        }

        return sb.toString();
    }

    private void appendCodeBlock(StringBuilder sb, BlogCodeBlockModel cb) {
        if (!cb.getDescription().isBlank()) {
            sb.append("**").append(cb.getDescription()).append("**\n\n");
        }
        sb.append("```").append(cb.getLanguage()).append("\n");
        sb.append(cb.getCode()).append("\n");
        sb.append("```\n\n");
    }

    private String escapeYaml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "\\\"");
    }
}
