package io.github.blogify.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BlogSectionModel implements Comparable<BlogSectionModel> {

    private final String heading;
    private final String content;
    private final int order;
    private final List<BlogCodeBlockModel> codeBlocks = new ArrayList<>();

    public BlogSectionModel(String heading, String content, int order) {
        this.heading = heading != null ? heading : "";
        this.content = content != null ? content : "";
        this.order = order;
    }

    public String getHeading() {
        return heading;
    }

    public String getContent() {
        return content;
    }

    public int getOrder() {
        return order;
    }

    public void addCodeBlock(BlogCodeBlockModel codeBlock) {
        if (codeBlock != null) {
            codeBlocks.add(codeBlock);
        }
    }

    public List<BlogCodeBlockModel> getCodeBlocks() {
        return Collections.unmodifiableList(codeBlocks);
    }

    @Override
    public int compareTo(BlogSectionModel o) {
        return Integer.compare(this.order, o.order);
    }
}
