package io.github.blogify.core.model;

public class BlogCodeBlockModel {

    private final String language;
    private final String description;
    private final String code;
    private final String fullCode;
    private final int order;

    public BlogCodeBlockModel(String language, String description, String code, int order) {
        this(language, description, code, "", order);
    }

    public BlogCodeBlockModel(String language, String description, String code, String fullCode, int order) {
        this.language = (language != null && !language.isBlank()) ? language : "java";
        this.description = description != null ? description : "";
        this.code = code != null ? code : "";
        this.fullCode = fullCode != null ? fullCode : "";
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

    public String getFullCode() {
        return fullCode;
    }

    public boolean hasFullCode() {
        return !fullCode.isBlank() && !fullCode.equals(code);
    }

    public int getOrder() {
        return order;
    }
}
