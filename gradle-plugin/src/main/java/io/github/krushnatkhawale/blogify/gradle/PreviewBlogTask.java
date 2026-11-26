package io.github.krushnatkhawale.blogify.gradle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Starts local interactive HTTP server")
public abstract class PreviewBlogTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<Integer> getPort();

    @Input
    @Optional
    public abstract Property<Boolean> getKeepAlive();

    @Input
    @Optional
    public abstract Property<String> getSiteUrl();

    @Option(option = "port", description = "Port to bind local preview HTTP server.")
    public void setPortOption(String portStr) {
        try {
            getPort().set(Integer.parseInt(portStr));
        } catch (NumberFormatException ignored) {
        }
    }

    @Option(option = "no-wait", description = "Start and stop immediately without waiting for user input.")
    public void setNoWaitOption(boolean noWait) {
        getKeepAlive().set(!noWait);
    }

    @TaskAction
    public void preview() throws IOException {
        File outDir = getOutputDir().getAsFile().get();
        File serveDir = outDir;

        // Check if there is a preview HTML subfolder (e.g. for github-pages or markdown formats)
        File previewSubDir = new File(outDir, "preview");
        if (new File(previewSubDir, "index.html").exists()) {
            serveDir = previewSubDir;
        } else if (!new File(outDir, "index.html").exists()) {
            getLogger().warn("[Blogify] No index.html found in {} or {}. Ensure 'generateBlog' ran first.",
                    outDir.getAbsolutePath(), previewSubDir.getAbsolutePath());
        }

        int requestedPort = getPort().getOrElse(8080);
        HttpServer server = null;
        int activePort = requestedPort;

        for (int p = requestedPort; p < requestedPort + 10; p++) {
            try {
                server = HttpServer.create(new InetSocketAddress(p), 0);
                activePort = p;
                break;
            } catch (IOException e) {
                // Try next port if already bound
            }
        }

        if (server == null) {
            throw new IOException("Failed to bind preview server on ports " + requestedPort + " to " + (requestedPort + 9));
        }

        final Path rootPath = serveDir.toPath().toAbsolutePath().normalize();
        server.createContext("/", new StaticFileHandler(rootPath));
        server.setExecutor(null);
        server.start();

        String url = "http://localhost:" + activePort + "/";
        String subpath = detectVersionedSubpath(serveDir);
        String blogLocalUrl = subpath.isBlank() ? url : "http://localhost:" + activePort + "/" + subpath;
        String deployedUrl = resolveDeployedBlogUrl(serveDir, subpath);

        getLogger().lifecycle("");
        getLogger().lifecycle("======================================================================");
        getLogger().lifecycle("  ⚡ Blogify Local Preview Server");
        if (!subpath.isBlank()) {
            getLogger().lifecycle("  💻 Localhost Blog URL: {}", blogLocalUrl);
            getLogger().lifecycle("  🏠 Localhost Root URL: {}", url);
        } else {
            getLogger().lifecycle("  💻 Localhost URL:      {}", url);
        }
        if (!deployedUrl.isBlank()) {
            getLogger().lifecycle("  🌐 Deployed Blog URL:  {}", deployedUrl);
            getLogger().lifecycle("  📋 Link for blogs.json: {}", deployedUrl);
        }
        getLogger().lifecycle("  Serving from:          {}", rootPath);
        getLogger().lifecycle("======================================================================");
        getLogger().lifecycle("");

        boolean keepAlive = getKeepAlive().getOrElse(true);
        if (keepAlive) {
            getLogger().lifecycle("[Blogify] Press Enter or Ctrl+C to stop the preview server...");
            try {
                int read = System.in.read();
            } catch (Exception ignored) {
            } finally {
                server.stop(0);
                getLogger().lifecycle("[Blogify] Preview server stopped.");
            }
        } else {
            // For tests or script execution
            server.stop(0);
            getLogger().lifecycle("[Blogify] Preview server test run completed.");
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

    private String resolveDeployedBlogUrl(File siteDir, String subpath) {
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
        return "";
    }

    private static class StaticFileHandler implements HttpHandler {
        private final Path rootPath;

        public StaticFileHandler(Path rootPath) {
            this.rootPath = rootPath;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath == null || requestPath.isBlank() || requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            // Path traversal protection
            Path targetFile = rootPath.resolve(requestPath.substring(1)).normalize();
            if (!targetFile.startsWith(rootPath)) {
                sendResponse(exchange, 403, "text/plain", "Forbidden".getBytes());
                return;
            }

            if (Files.isDirectory(targetFile)) {
                if (!requestPath.endsWith("/")) {
                    exchange.getResponseHeaders().set("Location", requestPath + "/");
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                    return;
                }
                targetFile = targetFile.resolve("index.html");
            }

            if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
                sendResponse(exchange, 404, "text/plain", ("404 Not Found: " + requestPath).getBytes());
                return;
            }

            String contentType = probeContentType(targetFile);
            byte[] bytes = Files.readAllBytes(targetFile);
            sendResponse(exchange, 200, contentType, bytes);
        }

        private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(statusCode, data.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(data);
            }
        }

        private String probeContentType(Path file) {
            String fileName = file.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                return "text/html; charset=UTF-8";
            } else if (fileName.endsWith(".css")) {
                return "text/css; charset=UTF-8";
            } else if (fileName.endsWith(".js")) {
                return "application/javascript; charset=UTF-8";
            } else if (fileName.endsWith(".json")) {
                return "application/json; charset=UTF-8";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".svg")) {
                return "image/svg+xml";
            } else if (fileName.endsWith(".md")) {
                return "text/markdown; charset=UTF-8";
            }
            return "text/plain; charset=UTF-8";
        }
    }
}
