# Multi-Module Projects

## Table of Contents

- [Overview](#overview)
- [Setup for a regular module](#setup-for-a-regular-module)
- [Setup for an annotation-alias module](#setup-for-an-annotation-alias-module)
- [Example layout](#example-layout)

## Overview

Hilt AutoBind works in multi-module projects. Each module that uses
`@AutoBinds`, `@AutoBindsIntoSet`, or `@AutoScoped` must have its own
KSP and Hilt setup. The annotation processor runs per-module and generates Hilt
modules locally.

The one exception is a module whose only purpose is to **define
[annotation aliases](annotation-aliases.md)**. Such a module does not
need a Hilt dependency because it contains no Hilt modules or bindings, but
only an annotation class declaration. KSP and the Hilt AutoBind compiler
are still required so the processor can generate the metadata that
downstream modules use to discover the alias.

## Setup for a Regular Module

Any module that annotates classes with `@AutoBinds`, `@AutoBindsIntoSet`,
or `@AutoScoped` needs the full setup: KSP plugin, Hilt, and both AutoBind dependencies.

```kotlin
// feature-module/build.gradle.kts
plugins {
    id("com.android.library") // or application, etc.
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

dependencies {
    implementation("com.google.dagger:hilt-android:<hilt-version>")
    ksp("com.google.dagger:hilt-android-compiler:<hilt-version>")

    implementation("com.uandcode:hilt-autobind:<version>")
    ksp("com.uandcode:hilt-autobind-compiler:<version>")
}
```

Additional configuration is not needed, so the configuration is actually
the same as for single-module projects. Annotate your classes exactly as
described in [Basic Usage](basic-usage.md) and the other guides. The
KSP processor handles everything within the module's own compilation unit.

## Setup for an Annotation-Alias Module

A module that only **defines** annotation aliases (and does not bind any
classes itself) does not need Hilt. KSP and the Hilt AutoBind compiler
are still required so the processor can generate the metadata carrier
classes that allow downstream modules to discover the alias.

```kotlin
// di-bindings-module/build.gradle.kts
plugins {
    id("com.android.library") // or kotlin("jvm") for a pure Kotlin module
    id("com.google.devtools.ksp")
}

dependencies {
    // No Hilt dependency needed here
    implementation("com.uandcode:hilt-autobind:<version>")
    ksp("com.uandcode:hilt-autobind-compiler:<version>")
    // javax-inject - for Inject annotation in Kotlin modules:
    implementation("javax.inject:javax.inject:1")
}
```

Define your alias in the module:

```kotlin
// di-bindings-module/src/main/.../DiBindings.kt
@Target(AnnotationTarget.CLASS)
@AutoBinds(factory = RetrofitBindingFactory::class)
annotation class BindRetrofit

@Target(AnnotationTarget.CLASS)
@AutoBinds(installIn = HiltComponent.Activity)
annotation class BindToActivity
```

And define factories, if needed:

```kotlin
class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {
    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }
}
```

Any downstream module that imports this module and applies `@BindRetrofit`
or `@BindToActivity` to its classes must itself have the full setup
(KSP + Hilt + Hilt AutoBind), as described in [Setup for a regular module](#setup-for-a-regular-module).

## Example Layout

Module list:

```
:app                         <- full setup (KSP + Hilt + AutoBind)
:feature:books               <- full setup
:feature:user                <- full setup
:di:bind-annotations         <- KSP + AutoBind aliases
```

Aliases and factories:

```kotlin
// :di:bind-annotations - defines the aliases and factories
@Target(AnnotationTarget.CLASS)
@AutoBinds(factory = RetrofitBindingFactory::class)
annotation class BindRetrofit

@Target(AnnotationTarget.CLASS)
@AutoBinds(installIn = HiltComponent.Activity)
annotation class BindToActivity

class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {
    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }
}
```

Other modules:

```kotlin
// :feature:books - uses the aliases; has full setup

@BindRetrofit
interface BooksApi {
    @GET("books")
    suspend fun getBooks(): List<BookDto>
}

@BindToActivity
class GestureControllerImpl : GestureController

// :feature:user - uses the alias as well; has full setup

@BindRetrofit
interface UserApi {
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Long): UserDto
}
```

Each feature module produces its own Hilt module (`BooksApiModule`,
`UserApiModule`) independently, without any coordination between modules.
