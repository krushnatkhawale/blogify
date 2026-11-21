---
title: "Blogify: A Blog Generator for Java-Based Applications"
slug: "blogify"
home: true
author: "Krushnat Khawale"
date: "2026-11-21"
description: "How we eliminated runtime dependencies and documentation drift by compiling story-first technical blogs directly from code and markdown."
tags: [java, blog-automation, gradle, static-site, gh-pages, documentation, open-source]
version: "0.1.0"
commit: "98ed70b"
repoUrl: "https://github.com/krushnatkhawale/blogify"
status: "active"
problem: |
  Why developers and tech bloggers struggle to keep blogs active:
  - ⏳ Writing Takes Too Long: Staring at a blank screen and drafting detailed technical posts takes hours.
  - 🔄 Code Changes, Blogs Don't: Software evolves quickly, leaving old blog posts filled with broken code.
  - 📋 Tedious Copy-Pasting: Moving code snippets between IDEs and blog platforms is clunky and error-prone.
  - ⚙️ Clunky Blog Tools: Learning separate web frameworks and wrestling with themes distracts from coding.
  - 🚀 Publishing Headaches: Setting up servers, hosting, and deploy scripts turns blogging into a chore.
insight: |
  Blogify solves this by automating technical blogging with AI directly inside your project.
  An AI assistant can draft architecture articles from your real code, Blogify keeps snippets in sync with Git, and a single Gradle command compiles and publishes a responsive website to GitHub Pages.
diagram: |
  flowchart TD
    subgraph Sources["1. Architecture and Blog Sources"]
      MD["blogs/{slug}/index.md<br/>(Markdown Leaf Bundles)"]
      Java["Java Code and Javadoc<br/>(@blog.* annotations / tags)"]
    end

    subgraph Plugin["2. Blogify Gradle Plugin (Zero Runtime)"]
      Scanner["BlogSourceScanner<br/>(Extracts frontmatter, metadata and snippets)"]
      Engine["BlogifyEngine<br/>(Compiles HTML, CSS, llms.txt)"]
      Themes["Multi-Theme Styler<br/>(Primer · Editorial · Minimal)"]
    end

    subgraph Outputs["3. Developer Deliverables"]
      Preview["previewBlog task<br/>(Local Server on port 8080)"]
      Publish["publishBlog task<br/>(Direct Git Push to gh-pages)"]
      AI["AI Agent Feeds<br/>(llms.txt and llms-full.txt)"]
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

## Why Tech Blogging Is Hard

![⏳ 1. Writing Takes Too Long (Hours Drafting From Scratch)](problem-1-writing-takes-too-long.jpg)
![🔄 2. Code Changes, Blogs Don't (Documentation Drift & Errors)](problem-2-code-changes-blogs-dont.jpg)
![📋 3. Tedious Copy-Pasting (Clunky Editors & Indentation Hell)](problem-3-tedious-copy-pasting.jpg)
![⚙️ 4. Clunky Blog Tools (NPM Dependency & Theme Hell)](problem-4-clunky-blog-tools.jpg)
![🚀 5. Publishing Headaches (Failed Pipelines & Deploy Headaches)](problem-5-publishing-headaches.jpg)

Most developers and tech bloggers start with great intentions. We want to share lessons learned, explain architecture decisions, and help other engineers. But maintaining a blog quickly becomes exhausting:

### ⏳ 1. Writing Takes Too Much Time
Drafting a technical guide from scratch—explaining the problem, structuring the narrative, and formatting code—often takes 4 to 8 hours. Between sprint deadlines and shipping features, few engineers have time for that.

### 🔄 2. Code Changes, Blogs Don't
Software is never static. A month after publishing, you refactor a service, upgrade a library, or change method signatures. Your blog now teaches outdated code that confuses readers.

### 📋 3. Copy-Pasting Code Snippets
Switching between your code editor and a web blog platform to copy, format, and re-indent code snippets is tedious and easily introduces syntax errors.

### ⚙️ 4. Blog Tools Are Too Complicated
Most blogging tools require learning separate website frameworks, installing extra runtimes, or wrestling with templates and CSS just to share a few paragraphs and code samples.

### 🚀 5. Publishing Is a Pain
Setting up hosting services, deployment keys, and continuous delivery scripts makes publishing feel like an operations project instead of simple writing.

---

## The Solution: AI-Automated Blogging with Blogify

Blogify was built to make technical blogging effortless by pairing **AI automation** with a **zero-overhead Gradle plugin**:

1. **Draft with AI in Seconds**: Feed your code or system design to your favorite AI assistant (ChatGPT, Claude, Cursor, Copilot) and ask it to draft a Blogify leaf bundle.
2. **Never Out of Sync**: Blog articles live right in your project repository under `blogs/`. Code snippets and architecture decisions stay anchored to your Git history.
3. **Zero Extra Tools to Install**: No Node.js, Ruby, or website generators needed. A single Gradle plugin handles responsive HTML generation, dark/light themes, and interactive Mermaid diagrams.
4. **Publish in One Command**: Run `./gradlew publishBlog` to build and deploy directly to GitHub Pages on demand—no servers, tokens, or complex setups required.

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
    participant Browser as Local Browser (port 8080)
    participant GH as GitHub Pages (gh-pages)

    Dev->>Gradle: ./gradlew previewBlog
    Gradle->>Plugin: Invoke PreviewBlogTask
    Plugin->>Scanner: Scan blogs/ bundles and Javadoc tags
    Scanner-->>Plugin: List of parsed BlogPostModel objects
    Plugin->>Engine: Compile HTML, CSS, themes and llms.txt
    Engine-->>Plugin: Static artifacts in build/blog/preview
    Plugin->>Browser: Start embedded HTTP daemon and open URL
    Browser-->>Dev: Live site with theme toggling and dark mode

    Dev->>Gradle: ./gradlew publishBlog
    Gradle->>Plugin: Invoke PublishBlogTask
    Plugin->>GH: Push build/blog/preview directly to gh-pages branch
    GH-->>Dev: Live site deployed to GitHub Pages
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
