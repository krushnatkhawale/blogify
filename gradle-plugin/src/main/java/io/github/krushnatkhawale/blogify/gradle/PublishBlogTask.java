package io.github.krushnatkhawale.blogify.gradle;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @Input
    @Optional
    public abstract Property<String> getSiteUrl();

    @Input
    @Optional
    public abstract Property<Integer> getPreviewPort();

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

        // 5. Detect versioned subpath, localhost URL, and deployed URL
        String subpath = detectVersionedSubpath(siteDir);
        int previewPort = getPreviewPort().getOrElse(8080);
        String localUrl = subpath.isBlank() ? "http://localhost:" + previewPort + "/" : "http://localhost:" + previewPort + "/" + subpath;
        String deployedUrl = resolveDeployedBlogUrl(siteDir, remoteUrl, subpath);

        getLogger().lifecycle("");
        getLogger().lifecycle("======================================================================");
        getLogger().lifecycle("  ⚡ Blogify On-Demand Publisher");
        getLogger().lifecycle("  Source Site:           {}", siteDir.getAbsolutePath());
        getLogger().lifecycle("  Remote:                {} ({})", remote, remoteUrl);
        getLogger().lifecycle("  Target Branch:         {}", branch);
        getLogger().lifecycle("  Commit Note:           {}", message);
        getLogger().lifecycle("  Dry Run:               {}", isDryRun ? "YES (simulation only)" : "NO (will push)");
        getLogger().lifecycle("----------------------------------------------------------------------");
        if (!deployedUrl.isBlank()) {
            getLogger().lifecycle("  🌐 Deployed Blog URL:  {}", deployedUrl);
        }
        getLogger().lifecycle("  💻 Localhost URL:      {}", localUrl);
        if (!deployedUrl.isBlank()) {
            getLogger().lifecycle("  📋 Link for blogs.json: {}", deployedUrl);
        }
        getLogger().lifecycle("======================================================================");
        getLogger().lifecycle("");

        // 6. Initialize isolated temporary git publication workspace
        File tempWorkDir = new File(getTemporaryDir(), "gh-pages-publish-" + System.currentTimeMillis());
        if (tempWorkDir.exists()) {
            deleteRecursively(tempWorkDir.toPath());
        }
        tempWorkDir.mkdirs();

        try {
            executeGitCommand(tempWorkDir, "init");
            executeGitCommand(tempWorkDir, "remote", "add", "origin", remoteUrl);

            boolean branchExists = false;
            try {
                CommandResult lsRemote = executeGitCommand(tempWorkDir, "ls-remote", "--heads", "origin", branch);
                if (!lsRemote.output.isBlank()) {
                    branchExists = true;
                }
            } catch (Exception ignored) {
            }

            if (branchExists) {
                getLogger().lifecycle("[Blogify] Fetching existing '{}' branch to merge publication safely...", branch);
                executeGitCommand(tempWorkDir, "fetch", "--depth=1", "origin", branch);
                executeGitCommand(tempWorkDir, "checkout", "-b", branch, "origin/" + branch);
            } else {
                getLogger().lifecycle("[Blogify] Initializing new orphan '{}' branch on remote...", branch);
                executeGitCommand(tempWorkDir, "checkout", "--orphan", branch);
            }

            // Non-destructively overlay files from siteDir into tempWorkDir
            overlayPublishDirectory(siteDir, tempWorkDir);

            // Ensure .nojekyll is present at target root
            File targetNoJekyll = new File(tempWorkDir, ".nojekyll");
            if (!targetNoJekyll.exists()) {
                Files.writeString(targetNoJekyll.toPath(), "# GitHub Pages: bypass Jekyll\n", StandardCharsets.UTF_8);
            }

            // Configure local committer name/email for this publication commit
            executeGitCommand(tempWorkDir, "config", "user.name", "Blogify Publisher");
            executeGitCommand(tempWorkDir, "config", "user.email", "blogify@users.noreply.github.com");

            executeGitCommand(tempWorkDir, "add", "-A");

            // Check if there is anything to commit
            CommandResult statusRes = executeGitCommand(tempWorkDir, "status", "--porcelain");
            if (statusRes.output.isBlank()) {
                getLogger().lifecycle("[Blogify] No changes detected in blog output. Publishing is already up to date.");
                getLogger().lifecycle("");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("  ⚡ Blog Already Up to Date");
                if (!deployedUrl.isBlank()) {
                    getLogger().lifecycle("  🌐 Deployed Blog URL:   {}", deployedUrl);
                }
                getLogger().lifecycle("  💻 Localhost URL:       {}", localUrl);
                if (!deployedUrl.isBlank()) {
                    getLogger().lifecycle("  📋 Link for blogs.json: {}", deployedUrl);
                }
                getLogger().lifecycle("  Tip: Run './gradlew previewBlog' to view locally at the localhost URL above.");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("");
                return;
            }

            executeGitCommand(tempWorkDir, "commit", "-m", message);

            if (isDryRun) {
                getLogger().lifecycle("[Blogify] [DRY RUN] Successfully prepared publication commit. Skipping 'git push'.");
                getLogger().lifecycle("");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("  ⚡ Publication Simulated Successfully (Dry Run)");
                if (!deployedUrl.isBlank()) {
                    getLogger().lifecycle("  🌐 Deployed Blog URL:   {}", deployedUrl);
                }
                getLogger().lifecycle("  💻 Localhost URL:       {}", localUrl);
                if (!deployedUrl.isBlank()) {
                    getLogger().lifecycle("  📋 Link for blogs.json: {}", deployedUrl);
                }
                getLogger().lifecycle("  Tip: Run './gradlew previewBlog' to test locally before pushing.");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("");
            } else {
                getLogger().lifecycle("[Blogify] Pushing static blog to {} ({branch})...", remoteUrl, branch);
                if (branchExists) {
                    executeGitCommand(tempWorkDir, "push", "origin", branch);
                } else {
                    executeGitCommand(tempWorkDir, "push", "-u", "origin", branch);
                }
                getLogger().lifecycle("");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("  🚀 Blog published successfully to branch '{}'!", branch);
                if (!deployedUrl.isBlank()) {
                    getLogger().lifecycle("  🌐 Deployed Blog URL:   {}", deployedUrl);
                }
                getLogger().lifecycle("  💻 Localhost URL:       {}", localUrl);
                if (!deployedUrl.isBlank()) {
                    getLogger().lifecycle("  📋 Link for blogs.json: {}", deployedUrl);
                }
                getLogger().lifecycle("  GitHub Pages will serve this branch automatically.");
                getLogger().lifecycle("  Tip: Run './gradlew previewBlog' to view locally at the localhost URL above.");
                getLogger().lifecycle("======================================================================");
                getLogger().lifecycle("");
            }
        } finally {
            if (tempWorkDir.exists()) {
                deleteRecursively(tempWorkDir.toPath());
            }
        }
    }

    private void overlayPublishDirectory(File srcDir, File destDir) {
        if (!srcDir.exists()) {
            return;
        }

        try (var stream = Files.walk(srcDir.toPath())) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> !p.toString().contains("/.git/"))
                  .forEach(p -> {
                      try {
                          Path relativePath = srcDir.toPath().relativize(p);
                          Path destPath = destDir.toPath().resolve(relativePath);

                          if ("blogs.json".equals(relativePath.toString()) && Files.exists(destPath)) {
                              // Safely merge existing blogs.json with incoming entries
                              mergeBlogsJson(p.toFile(), destPath.toFile());
                          } else {
                              Files.createDirectories(destPath.getParent());
                              Files.copy(p, destPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                          }
                      } catch (Exception e) {
                          getLogger().warn("[Blogify] Warning: failed to copy {} to publication: {}", p, e.getMessage());
                      }
                  });
        } catch (Exception e) {
            getLogger().warn("[Blogify] Warning traversing site directory: {}", e.getMessage());
        }
    }

    private void mergeBlogsJson(File srcJson, File destJson) {
        try {
            String srcContent = Files.readString(srcJson.toPath(), StandardCharsets.UTF_8).trim();
            String destContent = Files.readString(destJson.toPath(), StandardCharsets.UTF_8).trim();

            List<String> destEntries = parseJsonArrayEntries(destContent);
            List<String> srcEntries = parseJsonArrayEntries(srcContent);

            Map<String, String> mergedMap = new LinkedHashMap<>();
            for (String entry : destEntries) {
                String id = extractJsonField(entry, "id");
                if (!id.isBlank()) {
                    mergedMap.put(id, entry);
                } else {
                    mergedMap.put(entry, entry);
                }
            }
            for (String entry : srcEntries) {
                String id = extractJsonField(entry, "id");
                if (!id.isBlank()) {
                    mergedMap.put(id, entry);
                } else {
                    mergedMap.put(entry, entry);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            List<String> allEntries = new ArrayList<>(mergedMap.values());
            for (int i = 0; i < allEntries.size(); i++) {
                sb.append("  ").append(allEntries.get(i).replace("\n", "\n  "));
                if (i < allEntries.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("]\n");

            Files.writeString(destJson.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            try {
                Files.copy(srcJson.toPath(), destJson.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ignored) {
            }
        }
    }

    private List<String> parseJsonArrayEntries(String json) {
        List<String> list = new ArrayList<>();
        if (json == null || json.isBlank() || !json.contains("{")) {
            return list;
        }
        int depth = 0;
        int start = -1;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    list.add(json.substring(start, i + 1).trim());
                    start = -1;
                }
            }
        }
        return list;
    }

    private String extractJsonField(String objJson, String fieldName) {
        Pattern p = Pattern.compile("\"" + Pattern.quote(fieldName) + "\"\\s*:\\s*\"([^\"]*)\"");
        Matcher m = p.matcher(objJson);
        if (m.find()) {
            return m.group(1);
        }
        return "";
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

    private String detectVersionedSubpath(File siteDir) {
        File[] children = siteDir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                String name = child.getName();
                if ("preview".equals(name) || name.startsWith(".")) {
                    continue;
                }
                File[] subchildren = child.listFiles(File::isDirectory);
                if (subchildren != null) {
                    for (File subchild : subchildren) {
                        if (new File(subchild, "index.html").exists()) {
                            return name + "/" + subchild.getName() + "/";
                        }
                    }
                }
                if (new File(child, "index.html").exists()) {
                    return name + "/";
                }
            }
        }

        File blogsJson = new File(siteDir, "blogs.json");
        if (blogsJson.exists()) {
            try {
                String content = Files.readString(blogsJson.toPath(), StandardCharsets.UTF_8);
                Pattern linkPattern = Pattern.compile("\"link\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = linkPattern.matcher(content);
                if (m.find()) {
                    String link = m.group(1);
                    try {
                        java.net.URI uri = java.net.URI.create(link);
                        String path = uri.getPath();
                        if (path != null && !path.isBlank() && !path.equals("/")) {
                            if (path.startsWith("/")) {
                                path = path.substring(1);
                            }
                            return path.endsWith("/") ? path : path + "/";
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private String resolveDeployedBlogUrl(File siteDir, String remoteUrl, String subpath) {
        File blogsJson = new File(siteDir, "blogs.json");
        if (blogsJson.exists()) {
            try {
                String content = Files.readString(blogsJson.toPath(), StandardCharsets.UTF_8);
                Pattern linkPattern = Pattern.compile("\"link\"\\s*:\\s*\"([^\"]+)\"");
                Matcher m = linkPattern.matcher(content);
                if (m.find()) {
                    return m.group(1);
                }
            } catch (Exception ignored) {
            }
        }

        String siteUrlVal = getSiteUrl().getOrNull();
        if (siteUrlVal != null && !siteUrlVal.isBlank()) {
            String base = siteUrlVal.endsWith("/") ? siteUrlVal.substring(0, siteUrlVal.length() - 1) : siteUrlVal;
            return subpath.isBlank() ? base + "/" : base + "/" + subpath;
        }

        if (remoteUrl != null && remoteUrl.contains("github.com")) {
            String repoPart = "";
            if (remoteUrl.contains("github.com/")) {
                repoPart = remoteUrl.substring(remoteUrl.indexOf("github.com/") + "github.com/".length());
            } else if (remoteUrl.contains("github.com:")) {
                repoPart = remoteUrl.substring(remoteUrl.indexOf("github.com:") + "github.com:".length());
            }
            if (repoPart.endsWith(".git")) {
                repoPart = repoPart.substring(0, repoPart.length() - 4);
            }
            String[] parts = repoPart.split("/");
            if (parts.length >= 2) {
                String owner = parts[0].toLowerCase();
                String repo = parts[1];
                if (repo.equalsIgnoreCase(owner + ".github.io")) {
                    return "https://" + owner + ".github.io/" + subpath;
                } else {
                    return "https://" + owner + ".github.io/" + repo + "/" + subpath;
                }
            }
        }
        return subpath.isBlank() ? "" : "https://<owner>.github.io/" + subpath;
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
