package io.github.krushnatkhawale.blogify.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method, field, or class snippet to be rendered as a highlighted code block
 * in the generated Markdown blog post.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface BlogCodeBlock {

    /**
     * Language for the fenced markdown code block (e.g. "java", "json", "sql").
     */
    String language() default "java";

    /**
     * Optional caption or description above or below the code block.
     */
    String description() default "";

    /**
     * Whether to include the method signature and body.
     */
    boolean includeBody() default true;

    /**
     * Display order within its parent section or post.
     */
    int order() default 0;
}
