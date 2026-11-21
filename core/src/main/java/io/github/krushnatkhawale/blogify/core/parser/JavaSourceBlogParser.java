package io.github.krushnatkhawale.blogify.core.parser;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import io.github.krushnatkhawale.blogify.core.model.BlogCodeBlockModel;
import io.github.krushnatkhawale.blogify.core.model.BlogPostModel;
import io.github.krushnatkhawale.blogify.core.model.BlogSectionModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Parses Java source files using JavaParser AST to extract Blogify annotations,
 * Javadoc tags (@blog.title, @blog.section, @author, etc.), prose, and code blocks.
 */
public class JavaSourceBlogParser {

    public List<BlogPostModel> parse(Path javaFilePath) throws IOException {
        return parse(javaFilePath.toFile());
    }

    public List<BlogPostModel> parse(File javaFile) throws IOException {
        if (javaFile == null || !javaFile.exists() || !javaFile.getName().endsWith(".java")) {
            return Collections.emptyList();
        }

        CompilationUnit cu = StaticJavaParser.parse(javaFile);
        List<BlogPostModel> blogPosts = new ArrayList<>();

        for (ClassOrInterfaceDeclaration clazz : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            Optional<AnnotationExpr> blogPostAnno = findAnnotation(clazz.getAnnotations(), "BlogPost");
            if (blogPostAnno.isPresent()) {
                BlogPostModel model = buildClassBlogPostFromAnnotation(clazz, blogPostAnno.get());
                blogPosts.add(model);
            } else if (hasBlogJavadoc(clazz)) {
                BlogPostModel model = buildClassBlogPostFromJavadoc(clazz);
                blogPosts.add(model);
            }
        }

        return blogPosts;
    }

    private boolean hasBlogJavadoc(ClassOrInterfaceDeclaration clazz) {
        if (!clazz.getComment().isPresent()) {
            return false;
        }
        JavadocMetadata meta = JavadocMetadata.parse(clazz.getComment().get());
        return meta.hasAnyBlogTag() || meta.hasTag("@blog", "@blog.post", "@blog.title", "@blogpost");
    }

    /**
     * Builds a BlogPostModel from Javadoc comments without requiring any Java annotations.
     */
    private BlogPostModel buildClassBlogPostFromJavadoc(ClassOrInterfaceDeclaration clazz) {
        JavadocMetadata meta = JavadocMetadata.parse(clazz.getComment().orElse(null));

        String title = meta.getTag("@blog.title", "@blog.post");
        if (title.isBlank()) {
            title = humanizeIdentifier(clazz.getNameAsString());
        }

        String slug = meta.getTag("@blog.slug");
        if (slug.isBlank()) {
            slug = slugify(title);
        }

        String author = meta.getTag("@blog.author", "@author");
        String date = meta.getTag("@blog.date", "@since", "@version");
        if (date.isBlank() || !date.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            date = LocalDate.now().toString();
        }

        String description = meta.getTag("@blog.desc", "@blog.description");
        if (description.isBlank()) {
            description = meta.getFirstSentence();
        }

        boolean draft = meta.getBooleanTag(false, "@blog.draft");
        int order = meta.getIntTag(0, "@blog.order");

        BlogPostModel post = new BlogPostModel(title, slug, author, date, description, draft, order);

        // Tags
        List<String> tags = meta.getCommaSeparatedTags("@blog.tags", "@blog.tag");
        post.addTags(tags);

        // Intro content
        post.setIntroContent(meta.getProse());

        // Story-First & Version Metadata
        post.setProblem(meta.getTag("@blog.problem"));
        post.setInsight(meta.getTag("@blog.insight"));
        post.addTakeaways(meta.getTags("@blog.takeaway", "@blog.takeaways"));
        post.setDiagram(meta.getTag("@blog.diagram"));
        post.setVersion(meta.getTag("@blog.version"));
        post.setCommitSha(meta.getTag("@blog.commit", "@blog.commitsha"));
        post.setRepoUrl(meta.getTag("@blog.repourl"));
        post.setStatus(meta.getTag("@blog.status"));
        post.setSupersededBy(meta.getTag("@blog.supersededby"));
        post.setSupersededMessage(meta.getTag("@blog.supersededmessage"));

        // Scan methods for sections and code blocks
        int methodSeq = 1;
        for (MethodDeclaration method : clazz.getMethods()) {
            // Check annotation first
            Optional<AnnotationExpr> sectionAnno = findAnnotation(method.getAnnotations(), "BlogSection");
            Optional<AnnotationExpr> codeBlockAnno = findAnnotation(method.getAnnotations(), "BlogCodeBlock");

            if (sectionAnno.isPresent()) {
                addSectionFromAnnotation(post, method, sectionAnno.get(), codeBlockAnno);
                continue;
            }

            // Check Javadoc
            if (method.getComment().isPresent()) {
                JavadocMetadata mMeta = JavadocMetadata.parse(method.getComment().get());
                if (mMeta.hasTag("@blog.ignore", "@blog.skip", "@hidden")) {
                    continue;
                }

                if (mMeta.hasTag("@blog.section", "@blog.codeblock") || method.isPublic()) {
                    String heading = mMeta.getTag("@blog.section");
                    if (heading.isBlank()) {
                        heading = humanizeIdentifier(method.getNameAsString());
                    }
                    String content = mMeta.getProse();
                    int sOrder = mMeta.getIntTag(methodSeq++, "@blog.order");

                    BlogSectionModel section = new BlogSectionModel(heading, content, sOrder);

                    String desc = mMeta.getTag("@blog.codeblock", "@blog.desc", "@blog.description");
                    if (desc.isBlank()) {
                        desc = mMeta.getFirstSentence();
                        if (desc.isBlank()) {
                            desc = heading;
                        }
                    }
                    String lang = mMeta.getTag("@blog.lang", "@blog.language");
                    if (lang.isBlank()) {
                        lang = "java";
                    }

                    // Extract clean method code without annotations or Javadoc comment clutter
                    MethodDeclaration cleanMethod = method.clone();
                    cleanMethod.removeComment();
                    cleanMethod.getAnnotations().clear();
                    String cleanCode = cleanMethod.toString().trim();

                    String snippet = mMeta.getTag("@blog.snippet");
                    String displayCode = !snippet.isBlank() ? snippet : cleanCode;
                    String fullCode = !snippet.isBlank() ? cleanCode : "";

                    BlogCodeBlockModel codeBlock = new BlogCodeBlockModel(lang, desc, displayCode, fullCode, sOrder);
                    section.addCodeBlock(codeBlock);
                    post.addSection(section);
                }
            }
        }

        // Scan fields for sections
        for (FieldDeclaration field : clazz.getFields()) {
            if (field.getComment().isPresent()) {
                JavadocMetadata fMeta = JavadocMetadata.parse(field.getComment().get());
                if (fMeta.hasTag("@blog.section")) {
                    String heading = fMeta.getTag("@blog.section");
                    String content = fMeta.getProse();
                    int sOrder = fMeta.getIntTag(0, "@blog.order");
                    post.addSection(new BlogSectionModel(heading, content, sOrder));
                }
            }
        }

        return post;
    }

    private BlogPostModel buildClassBlogPostFromAnnotation(ClassOrInterfaceDeclaration clazz, AnnotationExpr anno) {
        String title = getAnnotationString(anno, "title");
        if (title.isBlank()) {
            title = humanizeIdentifier(clazz.getNameAsString());
        }

        String slug = getAnnotationString(anno, "slug");
        if (slug.isBlank()) {
            slug = slugify(title);
        }

        String author = getAnnotationString(anno, "author");
        String date = getAnnotationString(anno, "date");
        if (date.isBlank()) {
            date = LocalDate.now().toString();
        }

        String description = getAnnotationString(anno, "description");
        boolean draft = getAnnotationBoolean(anno, "draft", false);
        int order = getAnnotationInt(anno, "order", 0);

        BlogPostModel post = new BlogPostModel(title, slug, author, date, description, draft, order);

        // Tags
        List<String> tags = getAnnotationStringArray(anno, "tags");
        post.addTags(tags);

        // Additional @BlogTag annotations
        for (AnnotationExpr tagAnno : clazz.getAnnotations()) {
            if (isAnnotationNamed(tagAnno, "BlogTag")) {
                String tagVal = getSingleOrMemberString(tagAnno, "value");
                if (!tagVal.isBlank()) {
                    post.addTag(tagVal);
                }
            }
        }

        // Class Javadoc as intro content
        clazz.getComment().ifPresent(comment -> {
            String cleanedComment = cleanJavadocComment(comment);
            post.setIntroContent(cleanedComment);
            if (post.getDescription().isBlank() && !cleanedComment.isBlank()) {
                post.addTags(Collections.emptyList());
            }
        });

        // Scan methods for sections and code blocks
        for (MethodDeclaration method : clazz.getMethods()) {
            Optional<AnnotationExpr> sectionAnno = findAnnotation(method.getAnnotations(), "BlogSection");
            Optional<AnnotationExpr> codeBlockAnno = findAnnotation(method.getAnnotations(), "BlogCodeBlock");

            if (sectionAnno.isPresent()) {
                addSectionFromAnnotation(post, method, sectionAnno.get(), codeBlockAnno);
            } else if (codeBlockAnno.isPresent()) {
                BlogCodeBlockModel codeBlock = buildMethodCodeBlock(method, codeBlockAnno.get());
                post.addStandaloneCodeBlock(codeBlock);
            } else if (method.getComment().isPresent()) {
                JavadocMetadata mMeta = JavadocMetadata.parse(method.getComment().get());
                if (mMeta.hasTag("@blog.section")) {
                    String heading = mMeta.getTag("@blog.section");
                    String content = mMeta.getProse();
                    int sOrder = mMeta.getIntTag(0, "@blog.order");
                    BlogSectionModel section = new BlogSectionModel(heading, content, sOrder);
                    String desc = mMeta.getTag("@blog.codeblock");
                    MethodDeclaration cleanMethod = method.clone();
                    cleanMethod.removeComment();
                    cleanMethod.getAnnotations().clear();
                    section.addCodeBlock(new BlogCodeBlockModel("java", desc.isBlank() ? heading : desc, cleanMethod.toString().trim(), sOrder));
                    post.addSection(section);
                }
            }
        }

        // Scan fields for sections
        for (FieldDeclaration field : clazz.getFields()) {
            Optional<AnnotationExpr> sectionAnno = findAnnotation(field.getAnnotations(), "BlogSection");
            if (sectionAnno.isPresent()) {
                AnnotationExpr sAnno = sectionAnno.get();
                String heading = getAnnotationString(sAnno, "heading");
                String content = getAnnotationString(sAnno, "content");
                if (content.isBlank() && field.getComment().isPresent()) {
                    content = cleanJavadocComment(field.getComment().get());
                }
                int sOrder = getAnnotationInt(sAnno, "order", 0);
                post.addSection(new BlogSectionModel(heading, content, sOrder));
            }
        }

        return post;
    }

    private void addSectionFromAnnotation(BlogPostModel post, MethodDeclaration method, AnnotationExpr sAnno, Optional<AnnotationExpr> codeBlockAnno) {
        String heading = getAnnotationString(sAnno, "heading");
        if (heading.isBlank()) {
            heading = humanizeIdentifier(method.getNameAsString());
        }
        String content = getAnnotationString(sAnno, "content");
        if (content.isBlank() && method.getComment().isPresent()) {
            content = cleanJavadocComment(method.getComment().get());
        }
        int sOrder = getAnnotationInt(sAnno, "order", 0);

        BlogSectionModel section = new BlogSectionModel(heading, content, sOrder);

        if (codeBlockAnno.isPresent()) {
            BlogCodeBlockModel codeBlock = buildMethodCodeBlock(method, codeBlockAnno.get());
            section.addCodeBlock(codeBlock);
        }

        post.addSection(section);
    }

    private BlogCodeBlockModel buildMethodCodeBlock(MethodDeclaration method, AnnotationExpr anno) {
        String lang = getAnnotationString(anno, "language");
        if (lang.isBlank()) {
            lang = "java";
        }
        String description = getAnnotationString(anno, "description");
        int order = getAnnotationInt(anno, "order", 0);
        boolean includeBody = getAnnotationBoolean(anno, "includeBody", true);

        String code;
        if (includeBody) {
            code = method.toString().trim();
        } else {
            code = method.getDeclarationAsString(true, true, true) + ";";
        }

        return new BlogCodeBlockModel(lang, description, code, order);
    }

    private Optional<AnnotationExpr> findAnnotation(List<AnnotationExpr> annotations, String name) {
        return annotations.stream()
                .filter(a -> isAnnotationNamed(a, name))
                .findFirst();
    }

    private boolean isAnnotationNamed(AnnotationExpr annotation, String name) {
        String annoName = annotation.getNameAsString();
        return annoName.equals(name) || annoName.endsWith("." + name);
    }

    private String getAnnotationString(AnnotationExpr anno, String member) {
        return getSingleOrMemberString(anno, member);
    }

    private String getSingleOrMemberString(AnnotationExpr anno, String member) {
        if (anno instanceof SingleMemberAnnotationExpr single) {
            if ("value".equals(member) || "title".equals(member) || "heading".equals(member)) {
                return extractLiteral(single.getMemberValue());
            }
        } else if (anno instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(member)) {
                    return extractLiteral(pair.getValue());
                }
            }
        }
        return "";
    }

    private boolean getAnnotationBoolean(AnnotationExpr anno, String member, boolean defaultVal) {
        if (anno instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(member)) {
                    return Boolean.parseBoolean(pair.getValue().toString());
                }
            }
        }
        return defaultVal;
    }

    private int getAnnotationInt(AnnotationExpr anno, String member, int defaultVal) {
        if (anno instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(member)) {
                    try {
                        return Integer.parseInt(pair.getValue().toString());
                    } catch (NumberFormatException ignored) {
                        return defaultVal;
                    }
                }
            }
        }
        return defaultVal;
    }

    private List<String> getAnnotationStringArray(AnnotationExpr anno, String member) {
        List<String> result = new ArrayList<>();
        if (anno instanceof NormalAnnotationExpr normal) {
            for (MemberValuePair pair : normal.getPairs()) {
                if (pair.getNameAsString().equals(member)) {
                    Expression expr = pair.getValue();
                    if (expr instanceof ArrayInitializerExpr arr) {
                        for (Expression e : arr.getValues()) {
                            result.add(extractLiteral(e));
                        }
                    } else {
                        result.add(extractLiteral(expr));
                    }
                }
            }
        }
        return result;
    }

    private String extractLiteral(Expression expr) {
        if (expr instanceof StringLiteralExpr str) {
            return str.getValue();
        }
        String s = expr.toString().trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private String cleanJavadocComment(Comment comment) {
        String text = comment.getContent();
        StringBuilder sb = new StringBuilder();
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("*")) {
                trimmed = trimmed.substring(1).trim();
            }
            if (!trimmed.startsWith("@")) {
                sb.append(trimmed).append("\n");
            }
        }
        return sb.toString().trim();
    }

    public static String slugify(String text) {
        if (text == null || text.isBlank()) {
            return "post-" + System.currentTimeMillis();
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public static String humanizeIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "";
        }
        String spaced = identifier.replaceAll("(?<!^)(?=[A-Z])", " ");
        if (Character.isLowerCase(spaced.charAt(0))) {
            spaced = Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
        }
        return spaced;
    }
}
