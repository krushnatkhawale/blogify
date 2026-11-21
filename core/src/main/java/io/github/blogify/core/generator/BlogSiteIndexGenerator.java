package io.github.blogify.core.generator;

import io.github.blogify.core.model.BlogPostModel;

import java.util.List;

public class BlogSiteIndexGenerator {

    public String generateIndex(String siteTitle, List<BlogPostModel> posts) {
        StringBuilder sb = new StringBuilder();

        String title = (siteTitle != null && !siteTitle.isBlank()) ? siteTitle : "Project Blog";

        sb.append("---\n");
        sb.append("title: \"").append(title).append("\"\n");
        sb.append("type: \"index\"\n");
        sb.append("---\n\n");

        sb.append("# ").append(title).append("\n\n");
        sb.append("Welcome! This blog is automatically generated from application source code at compile time.\n\n");

        if (posts.isEmpty()) {
            sb.append("*No blog posts found. Annotate your classes with `@BlogPost` to generate posts.*\n");
            return sb.toString();
        }

        sb.append("## Articles\n\n");
        for (BlogPostModel post : posts) {
            String filename = post.getSlug() + ".md";
            sb.append("### [").append(post.getTitle()).append("](").append(filename).append(")\n\n");

            if (!post.getDate().isBlank() || !post.getAuthor().isBlank()) {
                sb.append("*");
                if (!post.getDate().isBlank()) {
                    sb.append(post.getDate());
                }
                if (!post.getDate().isBlank() && !post.getAuthor().isBlank()) {
                    sb.append(" • ");
                }
                if (!post.getAuthor().isBlank()) {
                    sb.append("by ").append(post.getAuthor());
                }
                sb.append("*\n\n");
            }

            if (!post.getDescription().isBlank()) {
                sb.append(post.getDescription()).append("\n\n");
            }

            if (!post.getTags().isEmpty()) {
                sb.append("**Tags:** ");
                sb.append(String.join(", ", post.getTags().stream().map(t -> "`" + t + "`").toList()));
                sb.append("\n\n");
            }

            sb.append("---\n\n");
        }

        return sb.toString();
    }
}
