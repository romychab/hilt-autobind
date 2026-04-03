# Annotation Aliases

## Table of Contents

- [Overview](#overview)
- [Defining an alias](#defining-an-alias)
- [Forwarding installIn and scope](#forwarding-installin-and-scope)
- [Forwarding a qualifier](#forwarding-a-qualifier)
- [Forwarding a factory](#forwarding-a-factory)
- [Multiple classes with one alias](#multiple-classes-with-one-alias)
- [Multibinding in aliases](#multibinding-in-aliases)
- [Requirements for alias annotations](#requirements-for-alias-annotations)

## Overview

When many classes share the same `@AutoBinds(factory = ...)` or `installIn`
configuration, you can define a custom annotation that carries those parameters
as defaults. Applying that annotation then has the same effect as applying
`@AutoBinds` with the same arguments. The same alias mechanism is also supported
for `@AutoBindsIntoSet` and `@AutoBindsIntoMap`.

This is useful for reducing boilerplate when binding many classes through a
shared factory (e.g., Retrofit interfaces):

```kotlin
// Before
@AutoBinds(factory = RetrofitBindingFactory::class)
interface UserApi { /* ... */ }

@AutoBinds(factory = RetrofitBindingFactory::class)
interface PostApi { /* ... */ }

// After
@Target(AnnotationTarget.CLASS)
@AutoBinds(factory = RetrofitBindingFactory::class)
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

## Forwarding installIn and scope

You can pin the `installIn` component in the alias, or let the processor
auto-detect it from a scope annotation, or both:

```kotlin
// explicit component only
@Target(AnnotationTarget.CLASS)
@AutoBinds(installIn = HiltComponent.Activity)
annotation class BindToActivity

// scope annotation only - component is auto-detected from @ActivityScoped
@Target(AnnotationTarget.CLASS)
@AutoBinds
@ActivityScoped
annotation class BindToActivity

// both - scope annotation must be consistent with installIn
@Target(AnnotationTarget.CLASS)
@AutoBinds(installIn = HiltComponent.Activity)
@ActivityScoped
annotation class BindToActivity
```

All three forms installs every class annotated with `@BindToActivity`
into `ActivityComponent`. The last 2 examples also scope every class
(`@ActivityScoped` annotation is added to the generated Hilt module).

### Scope annotation on the alias

Placing a scope annotation (e.g. `@Singleton`, `@ActivityScoped`) directly on
the alias has two effects:

1. **Component auto-detection** - if `installIn` is not set, the processor
   infers the target component from the scope (same as with direct `@AutoBinds`).
2. **Scoped factory bindings** - for `ClassBindingFactory` and
   `DelegateBindingFactory`, the scope annotation is forwarded to the generated
   `@Provides` function, making the binding scoped.

For example, placing `@Singleton` on a `ClassBindingFactory` alias:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBinds(factory = RetrofitBindingFactory::class)
@Singleton
annotation class BindRetrofit
```

Generates:

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

This is an alternative to placing `@AutoScoped` on the factory's `create()`
method. Use the alias-level scope when you want different aliases to the same
factory to have different scopes.

### Scope conflicts

The annotated class can still carry its own scope annotation. If it matches the
alias scope it is redundant but accepted. If it conflicts, the processor emits a
compile-time error:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBinds
@Singleton
annotation class MySingletonBind

// ERROR: class is scoped to ActivityScoped but alias targets Singleton
@MySingletonBind
@ActivityScoped
class MainPresenter @Inject constructor() : Presenter
```

If `installIn` is left at its default (`HiltComponent.Unspecified`) and no
scope annotation is present on either the alias or the annotated class, the
processor defaults to `SingletonComponent`. See
[Scopes and Components](scopes-and-components.md) for the full mapping.

## Forwarding a Qualifier

A qualifier annotation (annotated with `@Qualifier`) can be placed on the alias so
every class using that alias gets the qualifier automatically:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RetrofitApi

@Target(AnnotationTarget.CLASS)
@AutoBinds
@RetrofitApi
annotation class BindRetrofit
```

As a result, every class annotated with `@BindRetrofit` generates a binding
with `@RetrofitApi` qualifier on the `@Binds` function. See [Qualifiers](qualifiers.md#qualifiers-on-annotation-aliases)
for conflict rules when both the alias and the annotated class carry qualifiers.

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

To make the factory binding scoped, you can either place `@AutoScoped` on the
factory method, or place a scope annotation directly on the alias. For more
details, see [Scope annotation on the alias](#scope-annotation-on-the-alias) above.

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
class RepoBImpl @Inject constructor() : RepoB
```

This generates `RepoAImplModule.kt` and `RepoBImplModule.kt` as separate files,
just as if each class had `@AutoBinds` applied directly.

## Multibinding in aliases

### Set multibinding

`@AutoBindsIntoSet` supports the same alias mechanism as `@AutoBinds`. Annotate
an annotation class with `@AutoBindsIntoSet` and every class annotated with that
alias is contributed to the multibinding `Set` automatically:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBindsIntoSet
annotation class ContributeToSet

@ContributeToSet
class LoggingInterceptor @Inject constructor() : Interceptor

@ContributeToSet
class AuthInterceptor @Inject constructor() : Interceptor
```

The generated files are named with the `__IntoSetModule` suffix, just as for
direct `@AutoBindsIntoSet` usage.

All parameters - `installIn`, `bindTo`, and scope annotations are forwarded
the same way as with `@AutoBinds` aliases:

```kotlin
// pin the component in the alias
@Target(AnnotationTarget.CLASS)
@AutoBindsIntoSet(installIn = HiltComponent.Activity)
annotation class ContributeActivityHandler

// auto-detect component from scope annotation on the alias
@Target(AnnotationTarget.CLASS)
@AutoBindsIntoSet
@ActivityScoped
annotation class ContributeActivityScopedHandler

// restrict which supertypes are contributed to the Set
@Target(AnnotationTarget.CLASS)
@AutoBindsIntoSet(bindTo = [Interceptor::class])
annotation class ContributeInterceptor
```

### Map multibinding

`@AutoBindsIntoMap` supports the same alias mechanism. You can bake a map key
annotation directly into the alias, or leave the key for each class to provide
on its own:

**Alias with a baked-in key** - every class using the alias contributes under
the same key:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBindsIntoMap
@StringKey("plugin")
annotation class ContributePlugin

@ContributePlugin
class MyPlugin @Inject constructor() : Plugin
// --> @StringKey("plugin") is forwarded automatically
```

**Alias without a key** - each class provides its own key annotation:

```kotlin
@Target(AnnotationTarget.CLASS)
@AutoBindsIntoMap
annotation class ContributeToMap

@ContributeToMap
@IntKey(42)
class MyHandler @Inject constructor() : Handler
```

**Alias without a key, class without a key** - the `@ClassKey` fallback is
applied automatically:

```kotlin
@ContributeToMap
class MyHandler @Inject constructor() : Handler
// --> @ClassKey(MyHandler::class) is applied automatically
```

**Key conflict** - if the alias carries a key and the annotated class also
carries a *different* key, the processor emits a compile-time error:

```kotlin
@ContributePlugin           // has @StringKey("plugin")
@IntKey(1)                  // different key type → COMPILE ERROR
class MyPlugin @Inject constructor() : Plugin
```

If both the alias and the class carry the same key annotation with the same
value, it is treated as a single key (no conflict).

The generated files are named with the `__IntoMapModule` suffix, just as for
direct `@AutoBindsIntoMap` usage. All parameters — `installIn`, `bindTo`, and
scope annotations — are forwarded the same way as with `@AutoBinds` aliases.

## Requirements for Alias Annotations

An annotation used as an `@AutoBinds`, `@AutoBindsIntoSet`, or `@AutoBindsIntoMap` alias must:

- Be an **annotation class** (declared with the `annotation class` keyword).
- Declare **`@Target(AnnotationTarget.CLASS)`** so it can be applied to classes and interfaces.
- Carry `@AutoBinds` or `@AutoBindsIntoSet` (with any desired parameters) directly on its declaration.

The processor emits a compile-time error if `@Target(AnnotationTarget.CLASS)` is
missing.
