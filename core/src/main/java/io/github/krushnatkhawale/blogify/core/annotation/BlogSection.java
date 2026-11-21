package io.github.krushnatkhawale.blogify.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a section/subheading within a blog post.
 * Can be placed on methods, fields, or inner classes.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface BlogSection {

    /**
     * Subheading text for this section.
     */
    String heading();

    /**
     * Optional explicit section body text in Markdown.
     * If empty, the Javadoc comment of the annotated element is used.
     */
    String content() default "";

    /**
     * Display order of this section within the post.
     */
    int order() default 0;
}
