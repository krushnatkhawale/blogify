package io.github.krushnatkhawale.blogify.core.parser;

import com.github.javaparser.ast.comments.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Extracts prose and block tags (e.g. @blog.title, @author, @since, @blog.section)
 * from Javadoc comments without requiring any Java annotations.
 */
public class JavadocMetadata {

    private final String prose;
    private final Map<String, List<String>> tags;

    public JavadocMetadata(String prose, Map<String, List<String>> tags) {
        this.prose = prose != null ? prose.trim() : "";
        this.tags = tags != null ? tags : Collections.emptyMap();
    }

    public static JavadocMetadata parse(Comment comment) {
        if (comment == null) {
            return new JavadocMetadata("", Collections.emptyMap());
        }
        return parse(comment.getContent());
    }

    public static JavadocMetadata parse(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            return new JavadocMetadata("", Collections.emptyMap());
        }

        StringBuilder proseBuilder = new StringBuilder();
        Map<String, List<String>> tags = new LinkedHashMap<>();

        String[] lines = rawContent.split("\r?\n");
        boolean encounteredTag = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.startsWith("*")) {
                line = line.substring(1).trim();
            }

            if (line.startsWith("@")) {
                encounteredTag = true;
                int spaceIdx = line.indexOf(' ');
                String tagName;
                String tagValue;
                if (spaceIdx == -1) {
                    tagName = line.toLowerCase(Locale.ROOT);
                    tagValue = "";
                } else {
                    tagName = line.substring(0, spaceIdx).toLowerCase(Locale.ROOT);
                    tagValue = line.substring(spaceIdx + 1).trim();
                }
                tags.computeIfAbsent(tagName, k -> new ArrayList<>()).add(tagValue);
            } else if (!encounteredTag) {
                // Ignore lines that are purely empty at the very beginning
                if (proseBuilder.length() > 0 || !line.isBlank()) {
                    proseBuilder.append(line).append("\n");
                }
            }
        }

        return new JavadocMetadata(proseBuilder.toString(), tags);
    }

    public String getProse() {
        return prose;
    }

    public String getFirstSentence() {
        if (prose.isBlank()) {
            return "";
        }
        int idx = prose.indexOf('.');
        if (idx != -1) {
            return prose.substring(0, idx + 1).replace("\n", " ").trim();
        }
        return prose.lines().findFirst().orElse("").trim();
    }

    public boolean hasTag(String... tagNames) {
        for (String name : tagNames) {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("@")) {
                normalized = "@" + normalized;
            }
            if (tags.containsKey(normalized)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyBlogTag() {
        for (String key : tags.keySet()) {
            if (key.equals("@blog") || key.startsWith("@blog.")) {
                return true;
            }
        }
        return false;
    }

    public String getTag(String... tagNames) {
        for (String name : tagNames) {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("@")) {
                normalized = "@" + normalized;
            }
            List<String> values = tags.get(normalized);
            if (values != null && !values.isEmpty()) {
                String first = values.get(0).trim();
                if (!first.isBlank()) {
                    return first;
                }
            }
        }
        return "";
    }

    public List<String> getTags(String... tagNames) {
        List<String> result = new ArrayList<>();
        for (String name : tagNames) {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("@")) {
                normalized = "@" + normalized;
            }
            List<String> values = tags.get(normalized);
            if (values != null) {
                for (String v : values) {
                    if (!v.isBlank()) {
                        result.add(v.trim());
                    }
                }
            }
        }
        return result;
    }

    public List<String> getCommaSeparatedTags(String... tagNames) {
        List<String> result = new ArrayList<>();
        List<String> raw = getTags(tagNames);
        for (String item : raw) {
            String[] parts = item.split(",");
            for (String p : parts) {
                String trimmed = p.trim();
                if (!trimmed.isBlank()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    public int getIntTag(int defaultVal, String... tagNames) {
        String val = getTag(tagNames);
        if (!val.isBlank()) {
            try {
                return Integer.parseInt(val.replaceAll("[^0-9-]", ""));
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultVal;
    }

    public boolean getBooleanTag(boolean defaultVal, String... tagNames) {
        for (String name : tagNames) {
            String normalized = name.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("@")) {
                normalized = "@" + normalized;
            }
            if (tags.containsKey(normalized)) {
                String val = getTag(normalized);
                if (val.isBlank() || val.equalsIgnoreCase("true")) {
                    return true;
                }
                if (val.equalsIgnoreCase("false")) {
                    return false;
                }
            }
        }
        return defaultVal;
    }
}
