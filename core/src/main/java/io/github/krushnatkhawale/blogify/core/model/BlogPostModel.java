package io.github.krushnatkhawale.blogify.core.model;

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

    // Story-First & Version Metadata
    private String problem = "";
    private String insight = "";
    private final List<String> takeaways = new ArrayList<>();
    private String diagram = "";
    private String version = "";
    private String commitSha = "";
    private String repoUrl = "";
    private String status = "active"; // "active" or "retired" / "deprecated"
    private String supersededBy = "";
    private String supersededMessage = "";
    private String rawBody = "";
    private boolean home;
    private String category = "";

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

    public String getProblem() {
        return problem;
    }

    public void setProblem(String problem) {
        this.problem = problem != null ? problem : "";
    }

    public String getInsight() {
        return insight;
    }

    public void setInsight(String insight) {
        this.insight = insight != null ? insight : "";
    }

    public List<String> getTakeaways() {
        return Collections.unmodifiableList(takeaways);
    }

    public void addTakeaway(String takeaway) {
        if (takeaway != null && !takeaway.isBlank()) {
            this.takeaways.add(takeaway.trim());
        }
    }

    public void addTakeaways(List<String> list) {
        if (list != null) {
            for (String s : list) {
                addTakeaway(s);
            }
        }
    }

    public String getDiagram() {
        return diagram;
    }

    public void setDiagram(String diagram) {
        this.diagram = diagram != null ? diagram : "";
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version != null ? version : "";
    }

    public String getCommitSha() {
        return commitSha;
    }

    public void setCommitSha(String commitSha) {
        this.commitSha = commitSha != null ? commitSha : "";
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl != null ? repoUrl : "";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status != null ? status : "active";
    }

    public boolean isRetired() {
        return "retired".equalsIgnoreCase(status) || "deprecated".equalsIgnoreCase(status);
    }

    public String getSupersededBy() {
        return supersededBy;
    }

    public void setSupersededBy(String supersededBy) {
        this.supersededBy = supersededBy != null ? supersededBy : "";
    }

    public String getSupersededMessage() {
        return supersededMessage;
    }

    public void setSupersededMessage(String supersededMessage) {
        this.supersededMessage = supersededMessage != null ? supersededMessage : "";
    }

    public String getRawBody() {
        return rawBody;
    }

    public void setRawBody(String rawBody) {
        this.rawBody = rawBody != null ? rawBody : "";
    }

    public boolean isHome() {
        return home;
    }

    public void setHome(boolean home) {
        this.home = home;
    }

    public String getCategory() {
        if (category != null && !category.isBlank()) {
            return category;
        }
        if (!tags.isEmpty()) {
            String firstTag = tags.iterator().next();
            if (!firstTag.isBlank()) {
                return Character.toUpperCase(firstTag.charAt(0)) + (firstTag.length() > 1 ? firstTag.substring(1) : "");
            }
        }
        return "Engineering";
    }

    public void setCategory(String category) {
        this.category = category != null ? category : "";
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
