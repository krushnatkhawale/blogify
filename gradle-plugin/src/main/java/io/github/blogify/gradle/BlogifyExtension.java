package io.github.blogify.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public abstract class BlogifyExtension {

    @Inject
    public BlogifyExtension() {
    }

    /**
     * Source directory containing Java code with @blog.* Javadoc tags.
     * Defaults to 'src/main/java'.
     */
    public abstract DirectoryProperty getSourceDir();

    /**
     * Dedicated blog content directory containing Markdown leaf bundles (e.g. 'blogs/<topic>/index.md').
     * Defaults to the root project's 'blogs' directory ('rootProject/blogs').
     */
    public abstract DirectoryProperty getBlogsDir();

    /**
     * Output directory where blog posts and site files will be generated.
     * Defaults to 'build/blog'.
     */
    public abstract DirectoryProperty getOutputDir();

    /**
     * Site title for index and configuration generation.
     * Defaults to '{projectName} Blog'.
     */
    public abstract Property<String> getSiteTitle();

    /**
     * Target platform/format: 'github-pages' (default, Jekyll format), 'html', or 'markdown'.
     */
    public abstract Property<String> getFormat();

    /**
     * Default visual theme: 'primer' (developer default), 'editorial' (literary), or 'minimal' (hacker).
     */
    public abstract Property<String> getTheme();

    /**
     * Git repository base URL (e.g. 'https://github.com/org/repo') used for commit links.
     */
    public abstract Property<String> getRepoUrl();

    /**
     * Local HTTP port used when running the 'previewBlog' task.
     * Defaults to 8080.
     */
    public abstract Property<Integer> getPreviewPort();

    /**
     * Whether to generate index file listing all articles.
     * Defaults to true.
     */
    public abstract Property<Boolean> getGenerateIndex();

    /**
     * Whether to include articles marked as draft (draft: true / @blog.draft).
     * Defaults to false.
     */
    public abstract Property<Boolean> getIncludeDrafts();

    /**
     * Target git branch used when publishing to GitHub Pages on demand.
     * Defaults to 'gh-pages'.
     */
    public abstract Property<String> getPublishBranch();

    /**
     * Git remote name or repository URL used when publishing to GitHub Pages.
     * Defaults to 'origin'.
     */
    public abstract Property<String> getPublishRemote();

    /**
     * Commit message used when publishing to the publishBranch.
     * Defaults to 'Deploy blog [commit {short-sha}] [skip ci]'.
     */
    public abstract Property<String> getPublishMessage();
}
