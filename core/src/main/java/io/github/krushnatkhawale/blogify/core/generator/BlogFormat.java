package io.github.krushnatkhawale.blogify.core.generator;

import java.util.Locale;

public enum BlogFormat {
    GITHUB_PAGES("github-pages"),
    HTML("html"),
    MARKDOWN("markdown");

    private final String value;

    BlogFormat(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static BlogFormat fromString(String str) {
        if (str == null || str.isBlank()) {
            return GITHUB_PAGES;
        }
        String normalized = str.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "html", "static-html" -> HTML;
            case "markdown", "md" -> MARKDOWN;
            case "github-pages", "githubpages", "gh-pages", "jekyll" -> GITHUB_PAGES;
            default -> GITHUB_PAGES;
        };
    }
}
