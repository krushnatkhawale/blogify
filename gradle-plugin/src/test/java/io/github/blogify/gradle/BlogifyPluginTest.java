package io.github.blogify.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogifyPluginTest {

    @Test
    void pluginRegistersExtensionAndTask() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("io.github.blogify");

        BlogifyExtension extension = (BlogifyExtension) project.getExtensions().findByName("blogify");
        assertNotNull(extension, "blogify extension should be registered");

        assertTrue(project.getTasks().findByName("generateBlog") instanceof GenerateBlogTask,
                "generateBlog task should be registered");
    }
}
