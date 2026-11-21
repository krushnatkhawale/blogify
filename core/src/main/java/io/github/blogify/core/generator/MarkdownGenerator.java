package io.github.blogify.core.generator;

import io.github.blogify.core.model.BlogCodeBlockModel;
import io.github.blogify.core.model.BlogPostModel;
import io.github.blogify.core.model.BlogSectionModel;

import java.util.ArrayList;
import java.util.List;

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
        if (!post.getVersion().isBlank()) {
            sb.append("version: \"").append(escapeYaml(post.getVersion())).append("\"\n");
        }
        if (!post.getCommitSha().isBlank()) {
            sb.append("commit: \"").append(escapeYaml(post.getCommitSha())).append("\"\n");
        }
        if (!post.getStatus().isBlank()) {
            sb.append("status: \"").append(escapeYaml(post.getStatus())).append("\"\n");
        }
        sb.append("---\n\n");

        // 2. Post Title
        sb.append("# ").append(post.getTitle()).append("\n\n");

        // 3. Metadata line (Author, Date, subtle Source Version badge)
        sb.append("> *");
        List<String> metaParts = new ArrayList<>();
        if (!post.getAuthor().isBlank()) {
            metaParts.add("By " + post.getAuthor());
        }
        if (!post.getDate().isBlank()) {
            metaParts.add("Published on " + post.getDate());
        }
        if (!post.getVersion().isBlank() || !post.getCommitSha().isBlank()) {
            String srcBadge = "Source: ";
            if (!post.getVersion().isBlank()) {
                srcBadge += post.getVersion();
            }
            if (!post.getCommitSha().isBlank()) {
                if (!post.getVersion().isBlank()) {
                    srcBadge += " · ";
                }
                if (!post.getRepoUrl().isBlank()) {
                    srcBadge += "[" + post.getCommitSha() + "](" + post.getRepoUrl() + "/commit/" + post.getCommitSha() + ")";
                } else {
                    srcBadge += "commit " + post.getCommitSha();
                }
            }
            metaParts.add(srcBadge);
        }
        sb.append(String.join(" | ", metaParts));
        sb.append("*\n\n");

        // 4. Retired / Deprecated Banner
        if (post.isRetired()) {
            sb.append("> [!WARNING]\n");
            sb.append("> **This architecture guide has been retired.**\n");
            if (!post.getSupersededMessage().isBlank()) {
                sb.append("> ").append(post.getSupersededMessage()).append("\n");
            }
            if (!post.getSupersededBy().isBlank()) {
                sb.append("> [Read updated guide: ").append(post.getSupersededBy()).append("](").append(post.getSupersededBy()).append(".html)\n");
            }
            sb.append("\n");
        }

        // 5. Problem & Insight Callout Cards
        if (!post.getProblem().isBlank()) {
            sb.append("> [!NOTE]\n");
            sb.append("> **The Problem**: ").append(post.getProblem()).append("\n\n");
        }

        if (!post.getInsight().isBlank()) {
            sb.append("> [!TIP]\n");
            sb.append("> **The Key Insight**: ").append(post.getInsight()).append("\n\n");
        }

        // 6. Visual Flow / Architecture Diagram
        if (!post.getDiagram().isBlank()) {
            sb.append("### Architecture Flow\n\n");
            String dia = post.getDiagram().trim();
            if (dia.startsWith("http://") || dia.startsWith("https://") || dia.endsWith(".png") || dia.endsWith(".svg") || dia.endsWith(".jpg")) {
                sb.append("![").append(post.getTitle()).append(" Architecture](").append(dia).append(")\n\n");
            } else {
                sb.append("```mermaid\n").append(dia).append("\n```\n\n");
            }
        }

        // 7. Intro / Javadoc content
        if (!post.getIntroContent().isBlank()) {
            sb.append(post.getIntroContent()).append("\n\n");
        }

        // 8. Raw Body if available (e.g. from standalone markdown)
        if (!post.getRawBody().isBlank()) {
            sb.append(post.getRawBody()).append("\n\n");
        }

        // 9. Standalone code blocks
        for (BlogCodeBlockModel cb : post.getStandaloneCodeBlocks()) {
            appendCodeBlock(sb, cb);
        }

        // 10. Sections
        for (BlogSectionModel section : post.getSections()) {
            sb.append("## ").append(section.getHeading()).append("\n\n");
            if (!section.getContent().isBlank()) {
                sb.append(section.getContent()).append("\n\n");
            }
            for (BlogCodeBlockModel cb : section.getCodeBlocks()) {
                appendCodeBlock(sb, cb);
            }
        }

        // 11. Key Takeaways
        if (!post.getTakeaways().isEmpty()) {
            sb.append("### Key Takeaways\n\n");
            for (String t : post.getTakeaways()) {
                sb.append("- ").append(t).append("\n");
            }
            sb.append("\n");
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

        if (cb.hasFullCode()) {
            sb.append("<details>\n");
            sb.append("<summary>🔍 View Full Implementation (").append(cb.getDescription().isBlank() ? "Code" : cb.getDescription()).append(")</summary>\n\n");
            sb.append("```").append(cb.getLanguage()).append("\n");
            sb.append(cb.getFullCode()).append("\n");
            sb.append("```\n\n");
            sb.append("</details>\n\n");
        }
    }

    private String escapeYaml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\"", "\\\"");
    }
}
