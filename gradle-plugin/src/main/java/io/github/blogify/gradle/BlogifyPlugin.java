package io.github.blogify.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

public class BlogifyPlugin implements Plugin<Project> {

    public static final String EXTENSION_NAME = "blogify";
    public static final String TASK_NAME = "generateBlog";

    @Override
    public void apply(Project project) {
        BlogifyExtension extension = project.getExtensions().create(EXTENSION_NAME, BlogifyExtension.class);

        // Configure sensible conventions
        extension.getSourceDir().convention(
                project.getLayout().getProjectDirectory().dir("src/main/java")
        );
        extension.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("blog")
        );
        extension.getSiteTitle().convention(
                project.getName() + " Blog"
        );
        extension.getGenerateIndex().convention(true);

        TaskProvider<GenerateBlogTask> taskProvider = project.getTasks().register(TASK_NAME, GenerateBlogTask.class, task -> {
            task.setGroup("documentation");
            task.setDescription("Generates static Markdown blog posts from annotated Java source code at build time.");
            task.getSourceDir().set(extension.getSourceDir());
            task.getOutputDir().set(extension.getOutputDir());
            task.getSiteTitle().set(extension.getSiteTitle());
            task.getGenerateIndex().set(extension.getGenerateIndex());
        });

        // Automatically wire after Java compilation if Java plugin is applied
        project.getPlugins().withType(JavaPlugin.class, javaPlugin -> {
            project.getTasks().named(JavaPlugin.CLASSES_TASK_NAME).configure(classesTask -> {
                classesTask.finalizedBy(taskProvider);
            });
        });
    }
}
