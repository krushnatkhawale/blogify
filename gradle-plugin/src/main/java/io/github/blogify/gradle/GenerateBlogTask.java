package io.github.blogify.gradle;

import io.github.blogify.core.generator.BlogifyEngine;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@CacheableTask
public abstract class GenerateBlogTask extends DefaultTask {

    @InputDirectory
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getSourceDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getSiteTitle();

    @Input
    public abstract Property<Boolean> getGenerateIndex();

    @TaskAction
    public void generate() throws IOException {
        File src = getSourceDir().getAsFile().getOrNull();
        File out = getOutputDir().getAsFile().get();

        if (src == null || !src.exists()) {
            getLogger().info("[Blogify] Source directory {} does not exist, skipping blog generation.", src);
            return;
        }

        getLogger().lifecycle("[Blogify] Scanning {} for @BlogPost annotations...", src.getAbsolutePath());
        BlogifyEngine engine = new BlogifyEngine();
        List<Path> generated = engine.generate(
                Collections.singletonList(src),
                out,
                getSiteTitle().get(),
                getGenerateIndex().get()
        );

        getLogger().lifecycle("[Blogify] Generated {} blog artifact(s) into {}", generated.size(), out.getAbsolutePath());
        for (Path p : generated) {
            getLogger().info("[Blogify] - {}", p.getFileName());
        }
    }
}
