package io.github.blogify.gradle;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogifyPluginTest {

    @Test
    void pluginRegistersExtensionAndTask() {
        Project project = ProjectBuilder.builder().build();
        project.getPlugins().apply("java");
        project.getPlugins().apply("io.github.krushnatkhawale.blogify");

        BlogifyExtension extension = (BlogifyExtension) project.getExtensions().findByName("blogify");
        assertNotNull(extension, "blogify extension should be registered");
        assertEquals("github-pages", extension.getFormat().get());
        assertEquals(8080, extension.getPreviewPort().get());

        assertTrue(project.getTasks().findByName("generateBlog") instanceof GenerateBlogTask,
                "generateBlog task should be registered");

        assertTrue(project.getTasks().findByName("previewBlog") instanceof PreviewBlogTask,
                "previewBlog task should be registered");

        assertTrue(project.getTasks().findByName("publishBlog") instanceof PublishBlogTask,
                "publishBlog task should be registered");
        assertEquals("gh-pages", extension.getPublishBranch().get());
        assertEquals("origin", extension.getPublishRemote().get());
        assertEquals(false, extension.getIncludeDrafts().get());
    }
}
