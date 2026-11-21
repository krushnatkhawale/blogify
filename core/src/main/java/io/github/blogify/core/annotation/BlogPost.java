package io.github.blogify.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class or method to be transformed into a Markdown blog post at build time.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface BlogPost {

    /**
     * Title of the blog post. If empty, the class or method name is used.
     */
    String title() default "";

    /**
     * URL slug / file name for the blog post (without .md extension).
     * If empty, generated automatically from title.
     */
    String slug() default "";

    /**
     * Author of the blog post.
     */
    String author() default "";

    /**
     * Publication date (e.g., "YYYY-MM-DD" or ISO-8601). If empty, today's date is used.
     */
    String date() default "";

    /**
     * Categories or tags for classification.
     */
    String[] tags() default {};

    /**
     * Short excerpt or summary describing the post (used in frontmatter and index).
     */
    String description() default "";

    /**
     * Whether this post is marked as draft.
     */
    boolean draft() default false;

    /**
     * Explicit ordering index for sorting posts.
     */
    int order() default 0;
}
