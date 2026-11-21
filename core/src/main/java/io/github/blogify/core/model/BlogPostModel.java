package io.github.blogify.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BlogPostModel implements Comparable<BlogPostModel> {

    private final String title;
    private final String slug;
    private final String author;
    private final String date;
    private final String description;
    private final boolean draft;
    private final int order;
    private final Set<String> tags = new LinkedHashSet<>();
    private String introContent = "";
    private final List<BlogSectionModel> sections = new ArrayList<>();
    private final List<BlogCodeBlockModel> standaloneCodeBlocks = new ArrayList<>();

    public BlogPostModel(String title, String slug, String author, String date, String description, boolean draft, int order) {
        this.title = title != null ? title : "";
        this.slug = slug != null ? slug : "";
        this.author = author != null ? author : "";
        this.date = date != null ? date : "";
        this.description = description != null ? description : "";
        this.draft = draft;
        this.order = order;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDraft() {
        return draft;
    }

    public int getOrder() {
        return order;
    }

    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public void addTag(String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag.trim());
        }
    }

    public void addTags(List<String> newTags) {
        if (newTags != null) {
            for (String t : newTags) {
                addTag(t);
            }
        }
    }

    public String getIntroContent() {
        return introContent;
    }

    public void setIntroContent(String introContent) {
        this.introContent = introContent != null ? introContent : "";
    }

    public List<BlogSectionModel> getSections() {
        List<BlogSectionModel> sorted = new ArrayList<>(sections);
        Collections.sort(sorted);
        return Collections.unmodifiableList(sorted);
    }

    public void addSection(BlogSectionModel section) {
        if (section != null) {
            sections.add(section);
        }
    }

    public List<BlogCodeBlockModel> getStandaloneCodeBlocks() {
        return Collections.unmodifiableList(standaloneCodeBlocks);
    }

    public void addStandaloneCodeBlock(BlogCodeBlockModel codeBlock) {
        if (codeBlock != null) {
            standaloneCodeBlocks.add(codeBlock);
        }
    }

    @Override
    public int compareTo(BlogPostModel o) {
        int orderCmp = Integer.compare(this.order, o.order);
        if (orderCmp != 0) {
            return orderCmp;
        }
        return this.title.compareToIgnoreCase(o.title);
    }
}
