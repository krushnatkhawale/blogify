package io.github.blogify.core.model;

public class BlogCodeBlockModel {

    private final String language;
    private final String description;
    private final String code;
    private final int order;

    public BlogCodeBlockModel(String language, String description, String code, int order) {
        this.language = (language != null && !language.isBlank()) ? language : "java";
        this.description = description != null ? description : "";
        this.code = code != null ? code : "";
        this.order = order;
    }

    public String getLanguage() {
        return language;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public int getOrder() {
        return order;
    }
}
