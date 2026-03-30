# Installation

## Table of Contents

- [Prerequisites](#prerequisites)
- [Add KSP plugin](#add-ksp-plugin)
- [Add Hilt AutoBind dependencies](#add-hilt-autobind-dependencies)
- [Version compatibility](#version-compatibility)

## Prerequisites

Hilt AutoBind requires [Hilt](https://dagger.dev/hilt/) and
[KSP](https://github.com/google/ksp) to be set up in your project. If you
haven't configured them yet, follow the steps below.

### Hilt

Follow the [official Hilt setup guide](https://dagger.dev/hilt/gradle-setup)
to add the Hilt Gradle plugin and dependencies to your project.

### KSP

Follow the [KSP quickstart](https://github.com/google/ksp#quickstart) to add
the KSP Gradle plugin.

## Add KSP Plugin

Make sure the KSP plugin is applied in your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
    // ... other plugins (e.g., kotlin, hilt)
}
```

## Add Hilt AutoBind Dependencies

Add the runtime library and the KSP compiler to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.uandcode:hilt-autobind:0.4.1")
    ksp("com.uandcode:hilt-autobind-compiler:0.4.1")
}
```

### Using a version catalog

If your project uses a Gradle version catalog (`libs.versions.toml`):

```toml
[versions]
hiltAutoBind = "0.4.1"

[libraries]
hilt-autobind = { module = "com.uandcode:hilt-autobind", version.ref = "hiltAutoBind" }
hilt-autobind-compiler = { module = "com.uandcode:hilt-autobind-compiler", version.ref = "hiltAutoBind" }
```

Then reference them in `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.hilt.autobind)
    ksp(libs.hilt.autobind.compiler)
}
```

## Version Compatibility

| Dependency            | Minimum version |
|-----------------------|-----------------|
| Kotlin                | 2.0+            |
| KSP                   | 2.0+            |
| Dagger Hilt           | 2.50+           |
| Android Gradle Plugin | 8.0+            |
