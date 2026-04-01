# Installation

## Table of Contents

- [Prerequisites](#prerequisites)
- [Add Hilt AutoBind dependencies](#add-hilt-autobind-dependencies)
- [Version compatibility](#version-compatibility)

## Prerequisites

Hilt AutoBind requires [Hilt](https://dagger.dev/hilt/) and
[KSP](https://github.com/google/ksp) to be set up in your project. If you
haven't configured them yet, follow the steps below.

**1. Add the Hilt and KSP Gradle plugins to your root `build.gradle.kts`:**

```kotlin
plugins {
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.5" apply false
}
```

**2. Apply the plugins in your app module's `build.gradle.kts`:**

```kotlin
plugins {
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}
```

**3. Add the Hilt dependency and KSP compiler in the same module:**

```kotlin
dependencies {
    implementation("com.google.dagger:hilt-android:2.59.2")
    ksp("com.google.dagger:hilt-android-compiler:2.59.2")
}
```

**4. Annotate your `Application` class with `@HiltAndroidApp`:**

```kotlin
@HiltAndroidApp
class MyApplication : Application()
```

**5. Register it in `AndroidManifest.xml`:**

```xml
<application
    android:name=".MyApplication"
    ...>
</application>
```

For the full reference, see the:
1. [Official Hilt setup guide](https://dagger.dev/hilt/gradle-setup).
2. [KSP quickstart guide](https://github.com/google/ksp#quickstart).

## Add Hilt AutoBind Dependencies

Add the library and the KSP compiler to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.uandcode:hilt-autobind:0.6.0")
    ksp("com.uandcode:hilt-autobind-compiler:0.6.0")
}
```

### Using a version catalog

If your project uses a Gradle version catalog (`libs.versions.toml`):

```toml
[versions]
hiltAutoBind = "0.6.0"

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
