# Blogify

A compile-time Gradle plugin and core Java library that automatically generates static Markdown blog posts directly from application source code and metadata annotations during the build phase.

## Key Features

- **Build-Time Generation**: Scans source code and annotations during compilation with zero runtime overhead or dependencies in production.
- **Decoupled Architecture**:
  - `core`: Pure Java standalone library for AST scanning, annotation parsing, and Markdown generation.
  - `gradle-plugin`: Thin Gradle wrapper using `java-gradle-plugin` that integrates with the Gradle build lifecycle.
- **Rich Metadata Extraction**:
  - Extracts title, slug, author, date, categories, and tags into YAML frontmatter.
  - Converts Javadoc comments into blog prose.
  - Automatically formats annotated methods or fields into fenced code snippets.
  - Generates an `index.md` catalog of all published articles.

## Project Structure

```
blogify/
├── core/                  # Standalone Java library (AST parsing, annotations, Markdown generation)
├── gradle-plugin/         # Gradle plugin wrapper (io.github.blogify)
├── sample/                # Example consumer application demonstrating annotations
├── build.gradle           # Root Gradle build configuration (Groovy DSL)
└── settings.gradle        # Multi-module settings
```

## Quick Start

### 1. Apply the Plugin

In your `build.gradle`:

```groovy
plugins {
    id 'io.github.blogify' version '0.1.0'
}

dependencies {
    compileOnly 'io.github.blogify:core:0.1.0'
}
```

### 2. Annotate Your Java Code

```java
package com.example.service;

import io.github.blogify.core.annotation.BlogPost;
import io.github.blogify.core.annotation.BlogSection;
import io.github.blogify.core.annotation.BlogCodeBlock;

/**
 * Overview of the calculation engine architecture.
 */
@BlogPost(
    title = "High Performance Calculation Architecture",
    slug = "calculation-architecture",
    author = "Architecture Team",
    tags = {"performance", "java", "architecture"}
)
public class CalculationService {

    /**
     * Executes parallelized matrix computation across worker threads.
     */
    @BlogSection(heading = "Parallel Execution Strategy")
    @BlogCodeBlock(language = "java", description = "Worker thread orchestration")
    public void executeParallelTasks() {
        // Parallel computation logic
    }
}
```

### 3. Generate Static Blog

Run the custom task:

```bash
./gradlew generateBlog
```

The generated Markdown files will be placed in `build/blog/` (or your configured `outputDir`).

## Configuration

Configure the `blogify` extension in your `build.gradle`:

```groovy
blogify {
    sourceDir = file('src/main/java')
    outputDir = file('build/docs/blog')
    siteTitle = 'Engineering Blog'
    generateIndex = true
}
```

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
