package io.github.blogify.core.parser;

import io.github.blogify.core.model.BlogPostModel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses standalone Markdown blog posts from directory bundles (e.g. blogs/<slug>/index.md).
 */
public class MarkdownBlogParser {

    public BlogPostModel parse(File markdownFile) throws IOException {
        if (markdownFile == null || !markdownFile.exists() || !markdownFile.getName().endsWith(".md")) {
            return null;
        }

        String content = Files.readString(markdownFile.toPath(), StandardCharsets.UTF_8);
        return parseContent(content, markdownFile);
    }

    public BlogPostModel parseContent(String content, File markdownFile) {
        String slug = "";
        if (markdownFile != null) {
            File parent = markdownFile.getParentFile();
            if (parent != null && !parent.getName().equals("blogs")) {
                slug = parent.getName();
            } else {
                String name = markdownFile.getName();
                slug = name.endsWith(".md") ? name.substring(0, name.length() - 3) : name;
            }
        }

        Map<String, Object> frontmatter = new LinkedHashMap<>();
        String body = content;

        if (content.startsWith("---")) {
            int endIndex = content.indexOf("\n---", 3);
            if (endIndex != -1) {
                String yaml = content.substring(3, endIndex).trim();
                body = content.substring(endIndex + 4).trim();
                frontmatter = parseSimpleYaml(yaml);
            }
        }

        String title = getString(frontmatter, "title");
        if (title.isBlank()) {
            title = JavaSourceBlogParser.humanizeIdentifier(slug.replace('-', ' '));
        }

        String explicitSlug = getString(frontmatter, "slug");
        if (!explicitSlug.isBlank()) {
            slug = explicitSlug;
        }

        String author = getString(frontmatter, "author");
        String date = getString(frontmatter, "date");
        if (date.isBlank() || !date.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            date = LocalDate.now().toString();
        }

        String description = getString(frontmatter, "description");
        boolean draft = getBoolean(frontmatter, "draft", false);
        int order = getInt(frontmatter, "order", 0);
        boolean home = getBoolean(frontmatter, "home", false) || getBoolean(frontmatter, "isHome", false) || "index".equalsIgnoreCase(slug);

        BlogPostModel post = new BlogPostModel(title, slug, author, date, description, draft, order);
        post.setHome(home);

        // Tags
        List<String> tags = getList(frontmatter, "tags");
        post.addTags(tags);

        // Story-First & Version Metadata
        post.setProblem(getString(frontmatter, "problem"));
        post.setInsight(getString(frontmatter, "insight"));
        post.addTakeaways(getList(frontmatter, "takeaways"));
        post.setDiagram(getString(frontmatter, "diagram"));
        post.setVersion(getString(frontmatter, "version"));
        post.setCommitSha(getString(frontmatter, "commit", "commitSha", "gitCommit"));
        post.setRepoUrl(getString(frontmatter, "repoUrl", "repository"));
        post.setStatus(getString(frontmatter, "status"));
        post.setSupersededBy(getString(frontmatter, "supersededBy"));
        post.setSupersededMessage(getString(frontmatter, "supersededMessage"));

        post.setRawBody(body);

        return post;
    }

    private Map<String, Object> parseSimpleYaml(String yaml) {
        Map<String, Object> map = new LinkedHashMap<>();
        String[] lines = yaml.split("\r?\n");

        String currentListKey = null;
        List<String> currentList = null;

        String currentMultilineKey = null;
        StringBuilder multilineBuilder = null;

        for (String rawLine : lines) {
            String trimmed = rawLine.trim();
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                if (currentMultilineKey != null) {
                    multilineBuilder.append("\n");
                }
                continue;
            }

            // Check if indented under a multiline block or list
            boolean isIndented = rawLine.startsWith("  ") || rawLine.startsWith("\t");

            if (isIndented && currentMultilineKey != null) {
                multilineBuilder.append(rawLine.stripLeading()).append("\n");
                map.put(currentMultilineKey, multilineBuilder.toString().trim());
                continue;
            } else if (currentMultilineKey != null) {
                map.put(currentMultilineKey, multilineBuilder.toString().trim());
                currentMultilineKey = null;
                multilineBuilder = null;
            }

            if (trimmed.startsWith("- ") && currentListKey != null) {
                String item = trimmed.substring(2).trim();
                if ((item.startsWith("\"") && item.endsWith("\"")) || (item.startsWith("'") && item.endsWith("'"))) {
                    item = item.substring(1, item.length() - 1);
                }
                currentList.add(item);
                continue;
            }

            currentListKey = null;

            int colonIdx = trimmed.indexOf(':');
            if (colonIdx != -1) {
                String key = trimmed.substring(0, colonIdx).trim();
                String val = trimmed.substring(colonIdx + 1).trim();

                if (val.isEmpty()) {
                    currentListKey = key;
                    currentList = new ArrayList<>();
                    map.put(key, currentList);
                } else if (val.equals("|") || val.equals(">")) {
                    currentMultilineKey = key;
                    multilineBuilder = new StringBuilder();
                    map.put(key, "");
                } else if (val.startsWith("[") && val.endsWith("]")) {
                    String inside = val.substring(1, val.length() - 1);
                    List<String> items = new ArrayList<>();
                    for (String part : inside.split(",")) {
                        String s = part.trim();
                        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
                            s = s.substring(1, s.length() - 1);
                        }
                        if (!s.isBlank()) {
                            items.add(s);
                        }
                    }
                    map.put(key, items);
                } else {
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    map.put(key, val);
                }
            }
        }

        if (currentMultilineKey != null && multilineBuilder != null) {
            map.put(currentMultilineKey, multilineBuilder.toString().trim());
        }

        return map;
    }

    private String getString(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Object obj = map.get(k);
            if (obj != null) {
                String s = obj.toString().trim();
                if (!s.isBlank()) {
                    return s;
                }
            }
        }
        return "";
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultVal) {
        Object obj = map.get(key);
        if (obj != null) {
            return Boolean.parseBoolean(obj.toString().trim());
        }
        return defaultVal;
    }

    private int getInt(Map<String, Object> map, String key, int defaultVal) {
        Object obj = map.get(key);
        if (obj != null) {
            try {
                return Integer.parseInt(obj.toString().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(Map<String, Object> map, String key) {
        Object obj = map.get(key);
        if (obj instanceof List) {
            List<String> list = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item != null) {
                    list.add(item.toString());
                }
            }
            return list;
        } else if (obj instanceof String s && !s.isBlank()) {
            return Arrays.asList(s.split(",\\s*"));
        }
        return Collections.emptyList();
    }
}
