# Annotation Aliases

## Table of Contents

- [Overview](#overview)
- [Defining an alias](#defining-an-alias)
- [Forwarding installIn](#forwarding-installin)
- [Forwarding a factory](#forwarding-a-factory)
- [Multiple classes with one alias](#multiple-classes-with-one-alias)
- [Requirements for alias annotations](#requirements-for-alias-annotations)

## Overview

When many classes share the same `@AutoBinds(factory = ...)` or `installIn`
configuration, you can define a custom annotation that carries those parameters
as defaults. Applying that annotation then has the same effect as applying
`@AutoBinds` with the same arguments.

This is useful for reducing boilerplate when binding many classes through a
shared factory (e.g., Retrofit interfaces):

```kotlin
// Before
@AutoBinds(factory = RetrofitBindingFactory::class)
interface UserApi { /* ... */ }

@AutoBinds(factory = RetrofitBindingFactory::class)
interface PostApi { /* ... */ }

// After
@AutoBinds(factory = RetrofitBindingFactory::class)
@Target(AnnotationTarget.CLASS)
annotation class BindRetrofit

@BindRetrofit
interface UserApi { /* ... */ }

@BindRetrofit
interface PostApi { /* ... */ }
```

## Defining an Alias

Annotate a Kotlin annotation class with `@AutoBinds` (and optionally any
parameters you want to bake in). The alias must declare
`@Target(AnnotationTarget.CLASS)` so it can be applied to classes and interfaces:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBinds
annotation class MyBind
```

The processor detects this declaration and treats every class annotated with
`@MyBind` as if it were annotated with `@AutoBinds` directly.

## Forwarding installIn

The `installIn` component is fixed in the alias and automatically applied to
every class that uses it:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBinds(installIn = HiltComponent.Activity)
annotation class BindToActivity
```

```kotlin
@BindToActivity
class MainPresenter @Inject constructor() : Presenter
// same as: @AutoBinds(installIn = HiltComponent.Activity)
```

The annotated class can still carry a scope annotation. The processor validates
that the scope is consistent with the `installIn` value, exactly as it does for
direct `@AutoBinds` usage:

```kotlin
// OK: @ActivityScoped is consistent with installIn = Activity
@BindToActivity
@ActivityScoped
class MainPresenter @Inject constructor() : Presenter

// ERROR: @Singleton does not match installIn = Activity
@BindToActivity
@Singleton
class MainPresenter @Inject constructor() : Presenter
```

If `installIn` is left at its default (`HiltComponent.Unspecified`), the
processor auto-detects the component from the scope annotation on the annotated
class, same as with direct `@AutoBinds`. See
[Scopes and Components](scopes-and-components.md) for the full mapping.

## Forwarding a Factory

The `factory` parameter is forwarded to every class annotated with the alias,
so each class produces a module that delegates to that factory:

```kotlin
class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {

    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }
}

@Target(AnnotationTarget.CLASS)
@AutoBinds(factory = RetrofitBindingFactory::class)
annotation class BindRetrofit
```

```kotlin
@BindRetrofit
interface UserApi {
    @GET("users/{id}")
    suspend fun fetchUser(@Path("id") id: Long): UserDto
}

@BindRetrofit
interface PostApi {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>
}
```

**Generated code** (one module per interface):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object UserApiModule {
    @Provides
    @Singleton
    fun provideUserApi(factory: RetrofitBindingFactory): UserApi =
        factory.create(UserApi::class)
}
```

The alias works with all factory types: `ClassBindingFactory` (shown above) and
`DelegateBindingFactory`. See [Class Factory](class-factory.md) and
[Delegate Factory](delegate-factory.md) for details on each factory type.

## Multiple Classes with One Alias

All classes annotated with the same alias are processed independently. Each
produces its own Hilt module:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBinds
annotation class MyBind

@MyBind
class RepoAImpl @Inject constructor() : RepoA

@MyBind
class RepoBImpl @Inject constructor() : RepoBImpl
```

This generates `RepoAImplModule.kt` and `RepoBImplModule.kt` as separate files,
just as if each class had `@AutoBinds` applied directly.

## Requirements for Alias Annotations

An annotation used as an `@AutoBinds` alias must:

- Be an **annotation class** (declared with the `annotation class` keyword).
- Declare **`@Target(AnnotationTarget.CLASS)`** so it can be applied to classes and interfaces.
- Carry `@AutoBinds` (with any desired parameters) directly on its declaration.

The processor emits a compile-time error if `@Target(AnnotationTarget.CLASS)` is
missing.
