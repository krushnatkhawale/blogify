package io.github.krushnatkhawale.blogify.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.external.javadoc.StandardJavadocDocletOptions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class BlogifyPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "blogify";
    public static final String TASK_NAME = "generateBlog";
    public static final String PREVIEW_TASK_NAME = "previewBlog";
    public static final String PUBLISH_TASK_NAME = "publishBlog";

    @Override
    public void apply(Project project) {
        BlogifyExtension extension = project.getExtensions().create(EXTENSION_NAME, BlogifyExtension.class);

        // Configure sensible conventions
        extension.getSourceDir().convention(
                project.getLayout().getProjectDirectory().dir("src/main/java")
        );
        // Smart multi-module blogs directory resolution:
        // 1. rootProject/blogs/<projectName> (e.g. blogs/adapter)
        // 2. project/blogs
        // 3. rootProject/blogs
        File rootBlogs = project.getRootProject().file("blogs");
        File moduleBlogsInRoot = new File(rootBlogs, project.getName());
        File moduleLocalBlogs = project.file("blogs");

        if (project != project.getRootProject() && moduleBlogsInRoot.exists()) {
            extension.getBlogsDir().convention(
                    project.getLayout().getProjectDirectory().dir(moduleBlogsInRoot.getAbsolutePath())
            );
        } else if (moduleLocalBlogs.exists()) {
            extension.getBlogsDir().convention(
                    project.getLayout().getProjectDirectory().dir(moduleLocalBlogs.getAbsolutePath())
            );
        } else {
            extension.getBlogsDir().convention(
                    project.getRootProject().getLayout().getProjectDirectory().dir("blogs")
            );
        }

        extension.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("blog")
        );
        extension.getSiteTitle().convention(
                project.getName()
        );
        extension.getFormat().convention("github-pages");
        extension.getTheme().convention("primer");
        extension.getRepoUrl().convention(project.provider(() -> resolveGitOriginRepoUrl(project.getRootProject().getProjectDir())));
        extension.getPreviewPort().convention(8080);
        extension.getGenerateIndex().convention(true);
        extension.getIncludeDrafts().convention(false);
        extension.getPublishBranch().convention("gh-pages");
        extension.getPublishRemote().convention("origin");

        extension.getBlogHome().convention("");
        extension.getSiteUrl().convention(project.provider(() -> resolveGitPagesSiteUrl(project.getRootProject().getProjectDir())));
        extension.getGoogleAnalyticsId().convention("");
        extension.getEnableVisitorCounter().convention(false);
        extension.getCounterNamespace().convention("");

        TaskProvider<GenerateBlogTask> generateTaskProvider = project.getTasks().register(TASK_NAME, GenerateBlogTask.class, task -> {
            task.setGroup("documentation");
            task.setDescription("Generates static Markdown blog posts from Java source code and root blogs/ bundles.");
            task.getSourceDir().set(extension.getSourceDir());
            task.getBlogsDir().set(extension.getBlogsDir());
            task.getOutputDir().set(extension.getOutputDir());
            task.getSiteTitle().set(extension.getSiteTitle());
            task.getFormat().set(extension.getFormat());
            task.getTheme().set(extension.getTheme());
            task.getRepoUrl().set(extension.getRepoUrl());
            task.getGenerateIndex().set(extension.getGenerateIndex());
            task.getIncludeDrafts().set(extension.getIncludeDrafts());
            task.getBlogHome().set(extension.getBlogHome());
            task.getSiteUrl().set(extension.getSiteUrl());
            task.getGoogleAnalyticsId().set(extension.getGoogleAnalyticsId());
            task.getEnableVisitorCounter().set(extension.getEnableVisitorCounter());
            task.getCounterNamespace().set(extension.getCounterNamespace());
        });

        project.getTasks().register(PREVIEW_TASK_NAME, PreviewBlogTask.class, task -> {
            task.setGroup("documentation");
            task.setDescription("Starts an embedded local HTTP server to preview the generated blog in a browser.");
            task.dependsOn(generateTaskProvider);
            task.getOutputDir().set(extension.getOutputDir());
            task.getPort().set(extension.getPreviewPort());
            task.getSiteUrl().set(extension.getSiteUrl());
        });

        project.getTasks().register(PUBLISH_TASK_NAME, PublishBlogTask.class, task -> {
            task.setGroup("publishing");
            task.setDescription("Publishes the compiled static blog directly to GitHub Pages (gh-pages branch) on demand.");
            task.dependsOn(generateTaskProvider);
            task.getOutputDir().set(extension.getOutputDir());
            task.getPublishBranch().set(extension.getPublishBranch());
            task.getPublishRemote().set(extension.getPublishRemote());
            task.getPublishMessage().set(extension.getPublishMessage());
            task.getRepoUrl().set(extension.getRepoUrl());
            task.getSiteUrl().set(extension.getSiteUrl());
            task.getPreviewPort().set(extension.getPreviewPort());
        });

        // Ensure LLM authoring skill is present at rootProject/skills/blogify/SKILL.md
        ensureSkillFile(project.getRootProject());

        // Configure custom @blog.* Javadoc tags if Java plugin is applied (prevents unknown tag warnings)
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {

            // Register custom @blog.* Javadoc tags so standard Javadoc builds without unknown tag warnings
            project.getTasks().withType(Javadoc.class).configureEach(javadoc -> {
                if (javadoc.getOptions() instanceof StandardJavadocDocletOptions docletOptions) {
                    docletOptions.tags(
                            "blog:a:Blog",
                            "blog.title:a:Blog Title",
                            "blog.post:a:Blog Post",
                            "blog.slug:a:Blog Slug",
                            "blog.author:a:Blog Author",
                            "blog.date:a:Blog Date",
                            "blog.description:a:Blog Description",
                            "blog.desc:a:Blog Description",
                            "blog.tags:a:Blog Tags",
                            "blog.tag:a:Blog Tag",
                            "blog.order:a:Blog Order",
                            "blog.draft:a:Blog Draft",
                            "blog.problem:a:Blog Problem",
                            "blog.insight:a:Blog Insight",
                            "blog.takeaway:a:Blog Takeaway",
                            "blog.diagram:a:Blog Diagram",
                            "blog.version:a:Blog Version",
                            "blog.commit:a:Blog Commit",
                            "blog.status:a:Blog Status",
                            "blog.supersededBy:a:Blog Superseded By",
                            "blog.section:m:Blog Section",
                            "blog.codeblock:m:Blog Code Block",
                            "blog.snippet:m:Blog Snippet",
                            "blog.lang:m:Blog Language",
                            "blog.language:m:Blog Language",
                            "blog.ignore:m:Blog Ignore",
                            "blog.skip:m:Blog Skip"
                    );
                }
            });
        });
    }

    private void ensureSkillFile(Project rootProject) {
        try {
            File skillDir = rootProject.file("skills/blogify");
            File skillFile = new File(skillDir, "SKILL.md");
            if (!skillFile.exists()) {
                skillDir.mkdirs();
                String skillTemplate = """
                        ---
                        name: blogify
                        description: Author story-first engineering blogs with architecture diagrams, micro-snippets, and version snapshots.
                        ---

                        # Blogify Technical Blog Authoring Skill

                        When authoring articles for Blogify, always create a leaf bundle at:
                        `blogs/<topic-slug>/index.md`

                        ## Title Rules
                        - **Length**: Must be short: strictly 6 to 8 words.
                        - **Convey the Problem**: Must directly state or highlight the concrete engineering problem / failure mode being solved (e.g., "Preventing Duplicate Charges Under High Network Latency", "Handling Out-of-Memory Errors in Spring Batch", "Eliminating Connection Pool Exhaustion Under Spike Load").
                        - **Avoid Fluff**: Never use generic or vague titles like "How We Built X", "Introduction to Y", or lengthy academic descriptions.

                        ## Story-First Structure
                        1. **Problem**: Clear pain point or failure mode before this solution.
                        2. **Insight**: The non-obvious architecture pattern or design choice solving it.
                        3. **Diagram**: Mermaid sequence or flowchart visualizing component interactions.
                        4. **Micro-Snippets**: Focused 5-15 line code snippets illustrating the core trick.
                        5. **Takeaways**: 2-4 actionable lessons for system designers.
                        6. **Version Metadata**: Explicit git commit snapshot and release version to prevent documentation drift.

                        ## Workflow Rules: Generate vs Publish
                        - **Generate Only**: When asked to generate blogs or write content, ONLY run `./gradlew generateBlog` (or `./gradlew :<module>:generateBlog`).
                        - **Do NOT Publish Automatically**: NEVER run `./gradlew publishBlog` unless the user explicitly requests to publish or deploy to GitHub Pages. The user must review the generated blog first.
                        """;
                Files.writeString(skillFile.toPath(), skillTemplate, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    private static String resolveGitOriginRepoUrl(File dir) {
        String remote = resolveGitOriginRaw(dir);
        if (remote == null || remote.isBlank()) {
            return "";
        }
        if (remote.endsWith(".git")) {
            remote = remote.substring(0, remote.length() - 4);
        }
        if (remote.startsWith("git@github.com:")) {
            return "https://github.com/" + remote.substring("git@github.com:".length());
        }
        return remote;
    }

    private static String resolveGitPagesSiteUrl(File dir) {
        String remote = resolveGitOriginRaw(dir);
        if (remote == null || remote.isBlank()) {
            return "";
        }
        String path = "";
        if (remote.contains("github.com/")) {
            path = remote.substring(remote.indexOf("github.com/") + "github.com/".length());
        } else if (remote.contains("github.com:")) {
            path = remote.substring(remote.indexOf("github.com:") + "github.com:".length());
        }
        if (path.endsWith(".git")) {
            path = path.substring(0, path.length() - 4);
        }
        String[] parts = path.split("/");
        if (parts.length >= 2) {
            String owner = parts[0].toLowerCase();
            String repo = parts[1];
            if (repo.equalsIgnoreCase(owner + ".github.io")) {
                return "https://" + owner + ".github.io";
            }
            return "https://" + owner + ".github.io/" + repo;
        }
        return "";
    }

    private static String resolveGitOriginRaw(File dir) {
        try {
            Process p = new ProcessBuilder("git", "config", "--get", "remote.origin.url")
                    .directory(dir)
                    .start();
            try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line = r.readLine();
                return line != null ? line.trim() : "";
            }
        } catch (Exception ignored) {
            return "";
        }
    }
}
