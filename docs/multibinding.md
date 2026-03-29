# Multibinding

## Table of Contents

- [Overview](#overview)
- [Contributing to a Set](#contributing-to-a-set)
- [Combining with @AutoBinds](#combining-with-autobinds)
- [Choosing a component](#choosing-a-component)
- [Generated code](#generated-code)

## Overview

Use `@AutoBindsIntoSet` to contribute a class into a Dagger multibinding `Set`
of each directly implemented interface. This lets you collect multiple
implementations automatically without maintaining a manual module.

## Contributing to a Set

Annotate each implementation with `@AutoBindsIntoSet`:

```kotlin
@AutoBindsIntoSet
class LoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Chain): Response { /* ... */ }
}

@AutoBindsIntoSet
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Chain): Response { /* ... */ }
}
```

Now you can inject `Set<Interceptor>` anywhere and Hilt will collect all
contributions automatically:

```kotlin
class ApiClient @Inject constructor(
    private val interceptors: Set<@JvmSuppressWildcards Interceptor>,
) {
    // interceptors contains LoggingInterceptor and AuthInterceptor
}
```

## Combining with @AutoBinds

`@AutoBindsIntoSet` can be combined with `@AutoBinds` on the same class to
produce both a direct binding and a set contribution:

```kotlin
@AutoBinds            // provides Interceptor directly
@AutoBindsIntoSet     // also contributes to Set<Interceptor>
class DefaultInterceptor @Inject constructor() : Interceptor { /* ... */ }
```

This generates two separate modules with different names to avoid conflicts.

## Choosing a Component

`@AutoBindsIntoSet` supports the same `installIn` parameter as `@AutoBinds`.
It defaults to `HiltComponent.Unspecified` with auto-detection from scope
annotations:

```kotlin
@ActivityScoped
@AutoBindsIntoSet  // installed in ActivityComponent (auto-detected)
class ActivityInterceptor @Inject constructor() : Interceptor { /* ... */ }

@AutoBindsIntoSet(installIn = HiltComponent.ViewModel)
class ViewModelInterceptor @Inject constructor() : Interceptor { /* ... */ }
```

See [Scopes and Components](scopes-and-components.md) for the full reference.

## Generated Code

For each annotated class, the processor generates an `interface` Hilt module
with `@Binds @IntoSet` functions:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingInterceptor__IntoSetModule {
    @Binds @IntoSet
    fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
}
```

The same class requirements as [basic usage](basic-usage.md#requirements-for-annotated-classes)
apply: the class must be concrete, non-abstract, not an inner class, have `@Inject` on its
primary constructor, and implement at least one interface.
