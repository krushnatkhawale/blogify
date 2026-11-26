# Blogify

A zero-runtime, compile-time Gradle plugin and core Java engine that generates static engineering blogs from dedicated `blogs/` bundles and application source code.

## Key Features

- **Root `blogs/` Leaf Bundles**: Store articles in dedicated `blogs/<slug>/index.md` bundles committed to git. Keeps production JARs (`src/main/resources`) clean and co-locates images and architecture diagrams.
- **Zero Annotation Requirement**: Pure Markdown bundles or standard Javadoc `@blog.*` tags. Domain code requires zero compile-time dependencies on Blogify.
- **Story-First Engineering Narrative**: Problem ➔ Core Insight ➔ Architecture Diagram ➔ Micro-Snippets ➔ Key Takeaways.
- **Source Version & Git Commit Linking**: Subtle badge (`Source: v1.2.0 · commit 8f3b21a ↗`) linking code snippets to immutable GitHub commit snapshots to eliminate documentation drift.
- **Micro-Snippets & Full-Code Drawers**: Micro-snippets (5-15 lines) by default with expandable `<details>` drawers for full implementations.
- **Mermaid Architecture Diagrams**: Client-side rendering of sequence and flow diagrams directly from markdown fences or frontmatter.
- **3 Live Switchable Themes**:
  - `primer`: Developer default (clean GitHub docs style)
  - `editorial`: Publication-grade serif typography (Charter/Georgia, warm paper)
  - `minimal`: Stark hacker monochrome
  - Seamless Light/Dark mode toggle with `localStorage` persistence.
- **Blog Lifecycle & Retirement**: Mark older architectures with `status: "retired"` / `"deprecated"`, displaying warning callouts, catalog separation, and `supersededBy` links.
- **AI Discoverability**: Automatically generates `/llms.txt`, `/llms-full.txt`, permissive AI `robots.txt` (`GPTBot`, `ClaudeBot`, `PerplexityBot`), and Schema.org `TechArticle` JSON-LD data.
- **LLM Authoring Skill**: Out-of-the-box authoring skill at `skills/blogify/SKILL.md` for agents and LLMs.
- **Zero-Dependency Local Preview**: Embedded HTTP server (`./gradlew previewBlog`) with zero external SSG build dependencies.

---

## Project Structure

```
blogify/
├── blogs/                 # Dedicated root articles (leaf bundle: blogs/<slug>/index.md)
├── skills/blogify/        # LLM authoring skill (SKILL.md)
├── core/                  # Standalone Java generator (Markdown, HTML, Jekyll, AST parsers)
├── gradle-plugin/         # Gradle plugin wrapper (io.github.krushnatkhawale.blogify)
├── sample/                # Consumer module demonstrating zero runtime compile overhead
├── build.gradle           # Root Gradle build configuration
└── settings.gradle        # Multi-module settings
```

---

## Quick Start

### 1. Apply the Plugin

In your `build.gradle`:

```groovy
plugins {
    id 'io.github.krushnatkhawale.blogify' version '0.2.0'
}

blogify {
    siteTitle = 'Acme Engineering Blog'
    repoUrl = 'https://github.com/krushnatkhawale/blogify'
    theme = 'primer' // 'primer', 'editorial', or 'minimal'
}
```

### 2. Author a Story-First Article (`blogs/<slug>/index.md`)

```markdown
---
title: "Idempotent Order Processing Under Extreme Network Latency"
slug: "idempotent-order-processing"
author: "Distributed Systems Team"
date: "2026-11-21"
description: "How we achieved zero duplicate transactions using deterministic hash keys."
tags: [resilience, idempotency, distributed-systems]
version: "v1.2.0"
commit: "8f3b21a"
problem: |
  Payment networks occasionally drop TCP ACKs, causing upstream callers to retry requests and create duplicate charges.
insight: |
  Generating a deterministic SHA-256 fingerprint from natural business keys enforces exactly-once idempotency.
diagram: |
  sequenceDiagram
    Client->>Gateway: POST /orders
    Gateway->>LockManager: Acquire Lease
    Gateway-->>Client: 200 OK
takeaways:
  - "Never rely on the client clock for idempotency windows."
  - "Return identical HTTP 200 responses on retries."
---

## Technical Context
...
```

### 3. Or Document Pure Java with `@blog.*` Javadoc Tags

```java
/**
 * Order processing orchestration service.
 *
 * @author Lead Architect
 * @blog.title Designing an Idempotent Order Processing Pipeline
 * @blog.slug designing-idempotent-order-processing
 * @blog.version v1.2.0
 * @blog.commit 8f3b21a
 * @blog.problem Dropped TCP ACKs cause consumers to retry duplicate orders.
 * @blog.insight Deterministic payload fingerprinting enforces exactly-once execution.
 * @blog.takeaway Compute idempotency hashes from canonical business state.
 */
public class OrderService {
    /**
     * @blog.section Fulfillment Logic
     * @blog.codeblock Idempotent execution
     * @blog.snippet return "ORDER_FULFILLED:" + orderId + ":" + idempotencyKey;
     */
    public String fulfillOrder(String orderId, String idempotencyKey) {
        return "ORDER_FULFILLED:" + orderId + ":" + idempotencyKey;
    }
}
```

### 4. Build and Preview

```bash
# Generate static blog into build/blog/
./gradlew generateBlog

# Preview locally in your browser
./gradlew previewBlog
```

---

## Configuration Reference

```groovy
blogify {
    // Dedicated root directory for Markdown leaf bundles (default: rootProject/blogs)
    blogsDir = rootProject.file('blogs')

    // Source code directory for Javadoc tag scanning (default: src/main/java)
    sourceDir = file('src/main/java')

    // Destination for generated static site (default: build/blog)
    outputDir = file('build/blog')

    // Title of the blog site
    siteTitle = 'Acme Engineering Blog'

    // Git repository URL used for commit links
    repoUrl = 'https://github.com/my-org/my-service'

    // Default theme: 'primer' (developer default), 'editorial' (literary), or 'minimal' (hacker)
    theme = 'primer'

    // Format: 'github-pages' (Jekyll + preview), 'html' (standalone HTML/CSS), or 'markdown'
    format = 'github-pages'

    // Local port for 'previewBlog' task (default: 8080)
    previewPort = 8080

    // Whether to generate catalog index (default: true)
    generateIndex = true

    // Whether to include draft articles in output (default: false)
    includeDrafts = false

    // Target branch for GitHub Pages on-demand publishing (default: 'gh-pages')
    publishBranch = 'gh-pages'

    // Git remote or repository URL (default: 'origin' or repoUrl)
    publishRemote = 'origin'

    // Optional external blog home URL (e.g. portfolio blogs page 'https://krushnatkhawale.github.io/#/blogs')
    // When configured, navigation replaces separate 'Home' & 'Articles' links with a unified '← Back to Blogs'
    blogHome = 'https://krushnatkhawale.github.io/#/blogs'

    // Optional public canonical URL where this blog is hosted (e.g. 'https://krushnatkhawale.github.io/blogify/')
    // Used when generating portfolio cards in blogs.json (auto-derived from repoUrl if omitted)
    siteUrl = 'https://krushnatkhawale.github.io/blogify/'

    // Optional Google Analytics 4 Measurement ID (e.g. 'G-5BJKLL1EXD')
    // Injects official gtag.js with granular page_path and page_title tracking across all pages
    googleAnalyticsId = 'G-5BJKLL1EXD'

    // Optional on-page live visitor count badge (powered by CountAPI; cookieless, GDPR-compliant, no sign-up)
    enableVisitorCounter = true
    counterNamespace = 'my-blog-namespace' // Optional: auto-derived from repoUrl if omitted
}
```

---

## On-Demand Task Model (Zero Interference with Builds)

All Blogify tasks are strictly **on-demand**. They are **never** attached to standard Gradle lifecycle tasks (`compileJava`, `test`, `check`, `assemble`, or `build`):

| Task | Command | Description |
|---|---|---|
| **Generate** | `./gradlew generateBlog` | Compiles markdown & Javadoc into static blog files in `build/blog/` |
| **Preview** | `./gradlew previewBlog` | Boots local embedded HTTP server at `http://localhost:8080/` |
| **Publish** | `./gradlew publishBlog` | Builds and pushes directly to `gh-pages` branch on GitHub |

Running `./gradlew build` or pushing code to CI will **only** compile and test your project; it will **never** trigger any blogging tasks.

---

## Multi-Module Projects & Submodules

Blogify natively supports multi-module Java Gradle builds (e.g. `adapter`, `metadata-client`):

```groovy
// In root build.gradle:
plugins {
    id 'io.github.krushnatkhawale.blogify' version '0.3.0' apply false
}

subprojects {
    apply plugin: 'io.github.krushnatkhawale.blogify'

    blogify {
        blogHome = 'https://krushnatkhawale.github.io/#/blogs'
        format = 'html'
    }
}
```

### Module Content Resolution
Each submodule automatically checks for dedicated content:
1. `rootProject/blogs/<submoduleName>/index.md` (e.g. `blogs/adapter/index.md`)
2. `submodule/blogs/index.md`
3. Fallback to `rootProject/blogs/`

### Dynamic Site Title & Version Inference
`siteTitle` defaults to the submodule name, but can be seamlessly configured or overridden:
1. **CLI / Property**: `--site-title="My Title"` or `-PsiteTitle="My Title"`
2. **Metadata file (`blogify.info`)**: A simple properties file placed alongside your blog bundle:
   ```properties
   siteTitle = Spring Batch Integration Adapter
   version = 0.1.0
   author = Krushna Khawale
   category = Engineering
   tags = Spring Batch, Integration, ETL
   ```
3. **Markdown Frontmatter**: `siteTitle: "Spring Batch Integration Adapter"`
4. **Primary Post Title**: If unspecified, uses the title of the module's primary article.

### Versioned Slugs & GitHub Pages Safety
Each submodule generates and deploys to an immutable versioned directory:
```
<siteUrl>/<site-title-slug>/<version>/
# Example: https://krushnatkhawale.github.io/spring-batch-example/spring-batch-integration-adapter/0.1.0/
```
- **Non-Destructive Publishing**: When running `:submodule:publishBlog`, Blogify fetches the existing `gh-pages` branch and overlays only that submodule's `<slug>/<version>/` folder without clobbering sibling submodules or previous versions.
- **Unified `blogs.json`**: Portfolio feed entries across all submodules and versions are safely merged at the root of `gh-pages`.
- **Zero `home: true` Friction**: Single-article submodules automatically compile directly to `<slug>/<version>/index.html`, routing back to `blogHome` via top navbar brand and breadcrumbs.

---

## Publishing to GitHub Pages (Local & On-Demand)

Publish directly from your local terminal with **zero GitHub Actions required**:

```bash
./gradlew publishBlog
# Or for a specific submodule:
./gradlew :adapter:publishBlog
```

### What this does:
1. Compiles and generates the production static site (`index.html`, CSS, `.nojekyll`, `llms.txt`, `robots.txt`, `blogs.json`).
2. Creates an isolated temporary publication workspace.
3. Fetches the existing `gh-pages` branch, non-destructively overlays the new/updated version folder, and merges `blogs.json`.
4. Pushes safely to `gh-pages` on GitHub without force-wiping other submodules.
5. Leaves your local working tree and checked-out branch **completely untouched**.

### One-Time GitHub Setup
In your GitHub repository:
1. Navigate to **Settings** ➔ **Pages**.
2. Under **Build and deployment** ➔ **Source**, select **Deploy from a branch**.
3. Under **Branch**, select `gh-pages` and `/ (root)`.
4. Click **Save**.

Every time you run `./gradlew publishBlog`, GitHub Pages immediately deploys the update.

### Useful Command Options
```bash
# Preview what would be published without actually pushing:
./gradlew publishBlog --simulate

# Include draft articles in the published version:
./gradlew publishBlog --include-drafts

# Publish to a custom remote or branch:
./gradlew publishBlog --remote=origin --branch=gh-pages
```

---

## Continuous Integration (CI)

A lightweight CI workflow is provided at [`.github/workflows/build.yml`](.github/workflows/build.yml).

It automatically runs `./gradlew build` on every push and pull request to verify code compilation and tests. Because all blogging tasks are strictly on-demand, CI runs standard builds without generating or publishing blogs.

---

### Draft Articles
Articles marked with `draft: true` in frontmatter or `@blog.draft true` in Javadoc are strictly excluded from publication until you are ready. To include drafts for testing, pass `--include-drafts`.

---

## AI Discoverability (`llms.txt`)

Every Blogify build generates:
- **`llms.txt`**: Curated markdown manifest designed for LLMs, summarising all articles and architectural takeaways.
- **`llms-full.txt`**: Complete concatenated plain-text corpus enabling single-prompt full documentation ingest.
- **`robots.txt`**: Permissive configuration allowing `GPTBot`, `ClaudeBot`, and `PerplexityBot`.
- **JSON-LD Schema**: Embedded `TechArticle` structured data in every HTML page.

## Publishing the Plugin to the Gradle Plugin Portal

The Blogify Gradle plugin is officially published on the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/io.github.krushnatkhawale.blogify). Follow this procedure whenever releasing a new version.

### 1. Prerequisites & Credentials

You need an active publisher account on [plugins.gradle.org](https://plugins.gradle.org/).
Once logged in, obtain your API Key and Secret under your account settings.

Configure your credentials using **either** method:

* **Option A: User Gradle Properties (Recommended)**  
  In your home directory `~/.gradle/gradle.properties`:
  ```properties
  gradle.publish.key=YOUR_GRADLE_PUBLISH_KEY
  gradle.publish.secret=YOUR_GRADLE_PUBLISH_SECRET
  ```

* **Option B: Environment Variables**  
  ```bash
  export GRADLE_PUBLISH_KEY="YOUR_GRADLE_PUBLISH_KEY"
  export GRADLE_PUBLISH_SECRET="YOUR_GRADLE_PUBLISH_SECRET"
  ```

---

### 2. Local Terminal Publishing Procedure

#### Step 1: Bump the Version
In [`gradle.properties`](gradle.properties), update the version number (e.g., `version=0.2.0`):
```properties
group=io.github.krushnatkhawale.blogify
version=0.2.0
```
> [!NOTE]
> The Gradle Plugin Portal requires strictly increasing version numbers and rejects overwriting existing releases.

#### Step 2: Validate Builds & Tests
Ensure all unit and functional tests pass:
```bash
./gradlew check
```

#### Step 3: Publish to the Portal
Run the publication task:
```bash
./gradlew :gradle-plugin:publishPlugins
```
This builds `:core` and `:gradle-plugin`, signs the artifacts if signing keys are configured, and uploads the plugin marker and JARs to `plugins.gradle.org`. Within minutes, the new version becomes live for the JVM community.

#### Step 4: Tag & Push Git Release
Create and push an annotated Git release tag:
```bash
./gradlew createReleaseTag -Ppush
```
Or manually using Git:
```bash
git tag -a v0.2.0 -m "Release v0.2.0"
git push origin v0.2.0
```

---

### 3. Automated Publishing via GitHub Actions

A continuous delivery workflow is configured in [`.github/workflows/publish-plugin.yml`](.github/workflows/publish-plugin.yml):

1. Add your credentials as GitHub Repository Secrets (**Settings** ➔ **Secrets and variables** ➔ **Actions**):
   * `GRADLE_PUBLISH_KEY`
   * `GRADLE_PUBLISH_SECRET`
2. **Pushing a release tag** (`git push origin v0.2.0`) automatically kicks off the workflow, runs tests, and publishes the plugin.
3. You can also trigger a publication manually on-demand from GitHub's **Actions** tab (`workflow_dispatch`).

---

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
