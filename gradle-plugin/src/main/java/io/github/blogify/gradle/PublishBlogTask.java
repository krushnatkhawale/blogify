package io.github.blogify.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;

/**
 * Publishes the generated static blog directly to GitHub Pages (gh-pages branch)
 * from the local development environment on-demand without requiring GitHub Actions.
 */
@DisableCachingByDefault(because = "Publishes to remote git repository")
public abstract class PublishBlogTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<String> getPublishBranch();

    @Input
    public abstract Property<String> getPublishRemote();

    @Input
    @Optional
    public abstract Property<String> getPublishMessage();

    @Input
    @Optional
    public abstract Property<Boolean> getDryRun();

    @Input
    @Optional
    public abstract Property<String> getRepoUrl();

    @Option(option = "branch", description = "Target git branch for GitHub Pages publication (default: gh-pages)")
    public void setBranchOption(String branch) {
        getPublishBranch().set(branch);
    }

    @Option(option = "remote", description = "Git remote name or URL (default: origin)")
    public void setRemoteOption(String remote) {
        getPublishRemote().set(remote);
    }

    @Option(option = "message", description = "Custom commit message for published blog")
    public void setMessageOption(String message) {
        getPublishMessage().set(message);
    }

    @Option(option = "simulate", description = "Simulate publication without actually pushing to remote")
    public void setSimulateOption(boolean simulate) {
        getDryRun().set(simulate);
    }

    @TaskAction
    public void publish() throws IOException {
        File outDir = getOutputDir().getAsFile().get();
        File siteDir = outDir;

        // Determine the actual static site directory (preview directory contains compiled HTML/CSS/.nojekyll)
        File previewSubDir = new File(outDir, "preview");
        if (new File(previewSubDir, "index.html").exists()) {
            siteDir = previewSubDir;
        } else if (!new File(outDir, "index.html").exists()) {
            throw new GradleException("[Blogify] No index.html found in " + outDir.getAbsolutePath() +
                    ". Ensure 'generateBlog' runs first.");
        }

        // Ensure .nojekyll is present to bypass Jekyll processing on GitHub Pages
        File noJekyll = new File(siteDir, ".nojekyll");
        if (!noJekyll.exists()) {
            Files.writeString(noJekyll.toPath(), "# GitHub Pages: bypass Jekyll\n", StandardCharsets.UTF_8);
        }

        File rootProjectDir = getProject().getRootProject().getProjectDir();
        String remote = getPublishRemote().getOrElse("origin");
        String branch = getPublishBranch().getOrElse("gh-pages");
        boolean isDryRun = getDryRun().getOrElse(false);

        // 1. Verify git is available
        verifyGitAvailable(rootProjectDir);

        // 2. Resolve remote repository URL
        String remoteUrl = resolveRemoteUrl(rootProjectDir, remote);

        // 3. Obtain current source commit SHA for traceability
        String sourceSha = getSourceCommitSha(rootProjectDir);

        // 4. Determine commit message
        String message = getPublishMessage().getOrNull();
        if (message == null || message.isBlank()) {
            message = "Deploy blog" + (sourceSha.isBlank() ? "" : " [commit " + sourceSha + "]") + " [skip ci]";
        }

        getLogger().lifecycle("");
        getLogger().lifecycle("======================================================================");
        getLogger().lifecycle("  ⚡ Blogify On-Demand Publisher");
        getLogger().lifecycle("  Source Site:   {}", siteDir.getAbsolutePath());
        getLogger().lifecycle("  Remote:        {} ({})", remote, remoteUrl);
        getLogger().lifecycle("  Target Branch: {}", branch);
        getLogger().lifecycle("  Commit Note:   {}", message);
        getLogger().lifecycle("  Dry Run:       {}", isDryRun ? "YES (simulation only)" : "NO (will push)");
        getLogger().lifecycle("======================================================================");
        getLogger().lifecycle("");

        // 5. Initialize clean temporary git repository inside siteDir
        File gitDir = new File(siteDir, ".git");
        if (gitDir.exists()) {
            deleteRecursively(gitDir.toPath());
        }

        try {
            executeGitCommand(siteDir, "init");
            executeGitCommand(siteDir, "checkout", "-b", branch);
            executeGitCommand(siteDir, "add", "-A");

            // Check if there is anything to commit
            CommandResult statusRes = executeGitCommand(siteDir, "status", "--porcelain");
            if (statusRes.output.isBlank()) {
                getLogger().lifecycle("[Blogify] No changes detected in blog output. Publishing is already up to date.");
                return;
            }

            // Configure local committer name/email for this publication commit if not globally set
            executeGitCommand(siteDir, "config", "user.name", "Blogify Publisher");
            executeGitCommand(siteDir, "config", "user.email", "blogify@users.noreply.github.com");

            executeGitCommand(siteDir, "commit", "-m", message);

            if (isDryRun) {
                getLogger().lifecycle("[Blogify] [DRY RUN] Successfully prepared publication commit. Skipping 'git push'.");
            } else {
                getLogger().lifecycle("[Blogify] Pushing static blog to {} ({branch})...", remoteUrl, branch);
                executeGitCommand(siteDir, "push", "-f", remoteUrl, branch);
                getLogger().lifecycle("");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("  🚀 Blog published successfully to branch '{}'!", branch);
                getLogger().lifecycle("  GitHub Pages will serve this branch automatically.");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("");
            }
        } finally {
            // Clean up temporary .git directory to keep build output pristine
            if (gitDir.exists()) {
                deleteRecursively(gitDir.toPath());
            }
        }
    }

    private void verifyGitAvailable(File workingDir) {
        try {
            executeGitCommand(workingDir, "--version");
        } catch (Exception e) {
            throw new GradleException("[Blogify] Git is required to publish to GitHub Pages. Please ensure git is installed and in your PATH.", e);
        }
    }

    private String resolveRemoteUrl(File workingDir, String remote) {
        if (remote.startsWith("http://") || remote.startsWith("https://") || remote.startsWith("git@")) {
            return remote;
        }

        try {
            CommandResult result = executeGitCommand(workingDir, "remote", "get-url", remote);
            String url = result.output.trim();
            if (!url.isBlank()) {
                return url;
            }
        } catch (Exception ignored) {
        }

        try {
            CommandResult result = executeGitCommand(workingDir, "config", "--get", "remote." + remote + ".url");
            String url = result.output.trim();
            if (!url.isBlank()) {
                return url;
            }
        } catch (Exception ignored) {
        }

        String fallbackRepo = getRepoUrl().getOrNull();
        if (fallbackRepo != null && !fallbackRepo.isBlank()) {
            return fallbackRepo.endsWith(".git") ? fallbackRepo : fallbackRepo + ".git";
        }

        throw new GradleException("[Blogify] Could not resolve URL for git remote '" + remote +
                "'. Ensure you are in a git repository with an existing remote, or configure repoUrl in build.gradle, or specify --remote=<url>.");
    }

    private String getSourceCommitSha(File workingDir) {
        try {
            CommandResult result = executeGitCommand(workingDir, "rev-parse", "--short", "HEAD");
            return result.output.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private CommandResult executeGitCommand(File workingDir, String... args) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(args));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new GradleException("Git command failed (exit code " + exitCode + "): git " + String.join(" ", args) +
                        "\nOutput:\n" + output);
            }

            return new CommandResult(exitCode, output.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GradleException("Git execution interrupted", e);
        } catch (IOException e) {
            throw new GradleException("Failed to execute git command", e);
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

    private record CommandResult(int exitCode, String output) {}
}
