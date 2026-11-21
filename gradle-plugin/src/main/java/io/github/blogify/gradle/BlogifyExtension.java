package io.github.blogify.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class BlogifyExtension {

    @Inject
    public BlogifyExtension() {
    }

    /**
     * Source directory containing Java code with @BlogPost annotations.
     * Defaults to 'src/main/java'.
     */
    public abstract DirectoryProperty getSourceDir();

    /**
     * Output directory where Markdown blog posts will be generated.
     * Defaults to 'build/blog'.
     */
    public abstract DirectoryProperty getOutputDir();

    /**
     * Site title for index generation.
     * Defaults to '{projectName} Blog'.
     */
    public abstract Property<String> getSiteTitle();

    /**
     * Whether to generate index.md listing all articles.
     * Defaults to true.
     */
    public abstract Property<Boolean> getGenerateIndex();
}
