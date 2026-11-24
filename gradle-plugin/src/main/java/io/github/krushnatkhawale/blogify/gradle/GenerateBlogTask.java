package io.github.krushnatkhawale.blogify.gradle;

import io.github.krushnatkhawale.blogify.core.generator.BlogifyEngine;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.IgnoreEmptyDirectories;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@CacheableTask
public abstract class GenerateBlogTask extends DefaultTask {

    @InputFiles
    @Optional
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDir();

    @InputFiles
    @Optional
    @IgnoreEmptyDirectories
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getBlogsDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getSiteTitle();

    @Input
    public abstract Property<String> getFormat();

    @Input
    @Optional
    public abstract Property<String> getTheme();

    @Input
    @Optional
    public abstract Property<String> getRepoUrl();

    @Input
    public abstract Property<Boolean> getGenerateIndex();

    @Input
    @Optional
    public abstract Property<Boolean> getIncludeDrafts();

    @org.gradle.api.tasks.options.Option(option = "include-drafts", description = "Include draft articles in the generated output.")
    public void setIncludeDraftsOption(boolean include) {
        getIncludeDrafts().set(include);
    }

    @Input
    @Optional
    public abstract Property<String> getBlogHome();

    @org.gradle.api.tasks.options.Option(option = "blog-home", description = "URL of external blog home (e.g. portfolio blogs page).")
    public void setBlogHomeOption(String blogHome) {
        getBlogHome().set(blogHome);
    }

    @Input
    @Optional
    public abstract Property<String> getSiteUrl();

    @org.gradle.api.tasks.options.Option(option = "site-url", description = "Public base URL of this blog.")
    public void setSiteUrlOption(String siteUrl) {
        getSiteUrl().set(siteUrl);
    }

    @TaskAction
    public void generate() throws IOException {
        File src = getSourceDir().getAsFile().getOrNull();
        File blogs = getBlogsDir().getAsFile().getOrNull();
        File out = getOutputDir().getAsFile().get();

        List<File> scanDirs = new ArrayList<>();
        if (blogs != null && blogs.exists()) {
            scanDirs.add(blogs);
            getLogger().info("[Blogify] Included root blogs directory: {}", blogs.getAbsolutePath());
        }
        if (src != null && src.exists()) {
            scanDirs.add(src);
            getLogger().info("[Blogify] Included source directory: {}", src.getAbsolutePath());
        }

        if (scanDirs.isEmpty()) {
            getLogger().warn("[Blogify] Neither sourceDir ({}) nor blogsDir ({}) exists. Skipping generation.", src, blogs);
            return;
        }

        String format = getFormat().getOrElse("github-pages");
        String theme = getTheme().getOrElse("primer");

        getLogger().lifecycle("[Blogify] Scanning {} directory/directories for blog content (format: {}, theme: {})...",
                scanDirs.size(), format, theme);

        if (out.exists()) {
            deleteRecursively(out.toPath());
        }

        boolean includeDrafts = getIncludeDrafts().getOrElse(false);

        String blogHome = null;
        Object propBlogHome = getProject().findProperty("blogHome");
        if (propBlogHome != null && !propBlogHome.toString().isBlank()) {
            blogHome = propBlogHome.toString();
        } else {
            blogHome = getBlogHome().getOrNull();
        }

        String siteUrl = null;
        Object propSiteUrl = getProject().findProperty("siteUrl");
        if (propSiteUrl != null && !propSiteUrl.toString().isBlank()) {
            siteUrl = propSiteUrl.toString();
        } else {
            siteUrl = getSiteUrl().getOrNull();
        }

        BlogifyEngine engine = new BlogifyEngine();
        List<Path> generated = engine.generate(
                scanDirs,
                out,
                getSiteTitle().get(),
                getGenerateIndex().get(),
                format,
                theme,
                includeDrafts,
                blogHome,
                siteUrl
        );

        getLogger().lifecycle("[Blogify] Generated {} blog artifact(s) into {}", generated.size(), out.getAbsolutePath());
        for (Path p : generated) {
            getLogger().info("[Blogify] - {}", p.getFileName());
        }

        String portfolioJson = engine.getLastGeneratedPortfolioJson();
        if (portfolioJson != null && !portfolioJson.isBlank()) {
            getLogger().lifecycle("");
            getLogger().lifecycle("======================================================================");
            getLogger().lifecycle("  📋 Portfolio / Blog Hub JSON (for your frontend/src/data/blogs.json):");
            getLogger().lifecycle("======================================================================");
            getLogger().lifecycle(portfolioJson.trim());
            getLogger().lifecycle("======================================================================");
            getLogger().lifecycle("");
        }
    }

    private void deleteRecursively(Path path) {
        try {
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (Exception ignored) {
        }
    }
}
