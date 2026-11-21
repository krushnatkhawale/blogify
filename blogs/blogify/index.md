---
title: "Blogify: A Blog Generator for Java-Based Applications"
slug: "blogify"
home: true
author: "Krushnat Khawale"
date: "2026-11-21"
description: "How we eliminated runtime dependencies and documentation drift by compiling story-first technical blogs directly from code and markdown."
tags: [gradle, static-site, documentation, open-source, architecture]
version: "0.1.0"
commit: "98ed70b"
repoUrl: "https://github.com/krushnatkhawale/blogify"
status: "active"
problem: |
  Without a native, code-first static blogging tool in the Java/Gradle ecosystem, engineering teams face chronic friction:
  - Documentation Drift: Technical articles and architectural guides written on external platforms (Medium, Confluence, Notion) detach from reality as the codebase evolves, quickly becoming obsolete.
  - Foreign Toolchain Overhead: Traditional static site generators (Docusaurus, Astro, Hugo, Jekyll) force Java backend engineers to install and maintain Node.js, npm, Ruby, or Python toolchains in their repositories.
  - Production Artifact Bloat: Teams trying to serve technical documentation or release notes from within their application often pull heavy web frameworks into production Spring Boot JARs/WARs.
  - Fragile CI/CD Deployments: Automating blog publishing typically demands complex GitHub Actions workflows, external secret credentials, and fragile deployment pipelines.
  - Machine Invisibility: Most blogs lack structured AI discoverability, leaving architectural reasoning and design patterns invisible or un-indexable to modern LLM tools.
insight: |
  Blogify introduces a zero-runtime, compile-time Gradle static blog engine that lives right inside your build.
  It scans root blogs/ directory bundles and native Javadoc tags, generates responsive multi-theme HTML with Mermaid diagrams and immutable commit SHA badges, and publishes directly to GitHub Pages on demand with zero external dependencies.
diagram: |
  flowchart TD
    subgraph Sources["1. Architecture & Blog Sources"]
      MD["blogs/<slug>/index.md<br/>(Markdown Leaf Bundles)"]
      Java["Java Code & Javadoc<br/>(@blog.* annotations / tags)"]
    end

    subgraph Plugin["2. Blogify Gradle Plugin (Zero Runtime)"]
      Scanner["BlogSourceScanner<br/>(Extracts frontmatter, metadata & snippets)"]
      Engine["BlogifyEngine<br/>(Compiles HTML, CSS, llms.txt)"]
      Themes["Multi-Theme Styler<br/>(Primer · Editorial · Minimal)"]
    end

    subgraph Outputs["3. Developer Deliverables"]
      Preview["./gradlew previewBlog<br/>(Instant Local Server :8080)"]
      Publish["./gradlew publishBlog<br/>(Direct Git Push to gh-pages)"]
      AI["AI Agent Feeds<br/>(llms.txt & llms-full.txt)"]
    end

    MD --> Scanner
    Java --> Scanner
    Scanner --> Engine
    Themes --> Engine
    Engine --> Preview
    Engine --> Publish
    Engine --> AI
takeaways:
  - "Co-locate architecture blogs with the codebase in version control to eliminate documentation drift."
  - "Keep production lean: technical blogs should generate at compile time with zero runtime dependencies."
  - "AI discoverability via llms.txt ensures that modern LLMs index concise, token-efficient architecture context."
  - "Publishing should be frictionless: an on-demand task pushes directly to GitHub Pages without complex CI actions."
---

## Why Another Blog Generator?

In modern software engineering teams, technical blogging and architecture decision records often suffer from acute challenges when no dedicated, code-first tool exists:

1. **The Wiki Drift Trap**: Articles written on external platforms (Medium, Substack, Confluence) describe an architecture on day one. By day thirty, the code has evolved, and the blog now teaches outdated patterns with zero automated verification.
2. **The "JavaScript Tax" on Java Repositories**: Mainstream static site generators (Docusaurus, Gatsby, Next.js, Astro) require `package.json`, `node_modules`, npm lockfiles, and Node runtime in a backend Java repo. CI builds need Node setup steps, security scanners flag npm vulnerabilities, and backend developers must debug npm dependency conflicts just to publish docs.
3. **Production Artifact Bloat**: Teams attempting to serve documentation directly from their applications often pull heavy markdown rendering libraries or web controllers into production Spring Boot JARs, swelling artifact size and expanding the attack surface.
4. **Fragile CI/CD Pipelines**: Typical documentation hosting setups require complex GitHub Actions YAML workflows with personal access tokens (`PAT`), branch protection rules, and CI minute consumption.
5. **The Missing AI Knowledge Layer**: When coding assistants (Cursor, Copilot, ChatGPT, Claude) inspect repositories, architectural context is scattered or inaccessible without a standardized machine-readable manifest like `llms.txt`.

Blogify was built to solve all of these issues with a single Gradle plugin application: **zero runtime dependencies, zero external toolchains, and 100% on-demand execution.**

---

## The Story-First Narrative: Problem ➔ Insight ➔ Takeaways

Too much technical documentation is overwhelming—either drowning readers in 500 lines of boilerplate or generic prose generated by AI that lacks practical context.

Blogify enforces a **Story-First** structure:
- **⚠️ The Problem**: What was broken or failing before?
- **💡 The Core Insight**: What is the clever 20% algorithmic or architectural pattern solving it?
- **📐 Architecture Diagrams**: Native client-side Mermaid rendering for sequence and flow charts.
- **🚀 Key Takeaways**: High-signal lessons for system designers.

### Blogify Execution Sequence Workflow

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer / Tech Lead
    participant Gradle as Gradle Runner
    participant Plugin as Blogify Plugin
    participant Scanner as Source Scanner
    participant Engine as Blogify Engine
    participant Browser as Local Browser (:8080)
    participant GH as GitHub Pages (gh-pages)

    Dev->>Gradle: ./gradlew previewBlog
    Gradle->>Plugin: Invoke PreviewBlogTask
    Plugin->>Scanner: Scan blogs/ bundles & @blog.* Javadoc
    Scanner-->>Plugin: List<BlogPostModel>
    Plugin->>Engine: Compile HTML, CSS, themes & llms.txt
    Engine-->>Plugin: Static artifacts in build/blog/preview
    Plugin->>Browser: Start embedded HTTP daemon & open URL
    Browser-->>Dev: Live site with theme toggling & dark mode

    Dev->>Gradle: ./gradlew publishBlog
    Gradle->>Plugin: Invoke PublishBlogTask
    Plugin->>GH: Push build/blog/preview directly to gh-pages branch
    GH-->>Dev: 🚀 Live instantly at https://krushnatkhawale.github.io/blogify/
```

---

## 3 Switchable Themes on the Static UI

Blogify ships with three built-in visual themes switchable directly on the static site with an instant Dark/Light mode toggle:

1. **Primer** (Default): Clean GitHub developer docs style.
2. **Editorial**: Warm paper palette with publication-grade serif typography (Charter & Georgia).
3. **Minimal**: Stark hacker monochrome with crisp 1px borders.

Theme preferences are persisted client-side in `localStorage` without requiring cookies or server-side sessions.

---

## Micro-Snippets with Expandable Drawers

Instead of dumping huge files, authors can present concise micro-snippets (5–15 lines) showing the core algorithmic trick, with full implementations hidden inside collapsible `<details>` drawers:

```java
// BlogSourceScanner.java: zero-overhead scan of root blogs/ directory
public List<BlogPostModel> scanDirectory(File directory) {
    if (!directory.exists()) return Collections.emptyList();
    return Files.walk(directory.toPath())
            .filter(path -> path.getFileName().toString().equals("index.md"))
            .map(path -> markdownParser.parse(path.toFile()))
            .filter(Objects::nonNull)
            .sorted()
            .toList();
}
```

---

## AI Discoverability: Native `llms.txt`

Modern AI tools (ChatGPT, Claude, Grok, Perplexity) frequently crawl engineering documentation. Blogify automatically generates:
- **`/llms.txt`**: Curated markdown manifest listing articles, descriptions, and takeaways.
- **`/llms-full.txt`**: Complete concatenated text corpus for single-prompt LLM ingestion.
- **`robots.txt`**: Permissive configuration allowing AI crawlers (`GPTBot`, `ClaudeBot`, `PerplexityBot`).
- **JSON-LD**: Embedded `TechArticle` structured data in every page header.

---

## Quick Start in Any Gradle Project

Add the plugin to your `build.gradle`:

```groovy
plugins {
    id 'io.github.krushnatkhawale.blogify' version '0.1.0'
}

blogify {
    siteTitle = 'Engineering Blog'
    repoUrl = 'https://github.com/krushnatkhawale/blogify'
    theme = 'primer'
}
```

### On-Demand Commands

```bash
# Preview locally at http://localhost:8080/
./gradlew previewBlog

# Publish directly to GitHub Pages (gh-pages branch) on demand
./gradlew publishBlog
```
