# Map Multibinding

## Table of Contents

- [Overview](#overview)
- [Contributing to a Map](#contributing-to-a-map)
- [Choosing a map key](#choosing-a-map-key)
- [ClassKey fallback](#classkey-fallback)
- [Custom MapKey annotations](#custom-mapkey-annotations)
- [Combining with other annotations](#combining-with-other-annotations)
- [Choosing a component](#choosing-a-component)
- [Selecting specific binding targets](#selecting-specific-binding-targets)
- [Generated code](#generated-code)

## Overview

Use `@AutoBindsIntoMap` to contribute a class into a Dagger multibinding `Map`.
Each contribution requires a map key annotation (e.g. `@StringKey`, `@IntKey`,
`@ClassKey`, or any custom `@MapKey` annotation). The processor forwards the
key annotation to the generated `@Binds @IntoMap` function.

## Contributing to a Map

Annotate each implementation with `@AutoBindsIntoMap` and a map key annotation:

```kotlin
@AutoBindsIntoMap
@StringKey("logging")
class LoggingInterceptor @Inject constructor() : Interceptor

@AutoBindsIntoMap
@StringKey("auth")
class AuthInterceptor @Inject constructor() : Interceptor
```

Now you can inject `Map<String, Interceptor>` anywhere and Hilt will collect all
contributions automatically:

!!! warning
    The annotation `@JvmSuppressWildcards` is required when injecting a map.
    This is a current limitation of Dagger 2 library, since it generates Java
    source code under the hood.

```kotlin
class ApiClient @Inject constructor(
    private val interceptors: Map<String, @JvmSuppressWildcards Interceptor>,
) {
    // interceptors["logging"] == LoggingInterceptor
    // interceptors["auth"]    == AuthInterceptor
}
```

## Choosing a Map Key

Dagger provides several built-in key annotations:

| Annotation   | Map key type               | Example                                |
|--------------|----------------------------|----------------------------------------|
| `@StringKey` | `String`                   | `@StringKey("logging")`                |
| `@IntKey`    | `Int`                      | `@IntKey(1)`                           |
| `@LongKey`   | `Long`                     | `@LongKey(100L)`                       |
| `@ClassKey`  | `Class<*>` (not `KClass`!) | `@ClassKey(LoggingInterceptor::class)` |

Place exactly one key annotation alongside `@AutoBindsIntoMap`. The processor
emits a compile-time error if more than one `@MapKey`-annotated annotation is
present on the same class.

## ClassKey Fallback

If you omit a key annotation entirely, the processor automatically generates
`@ClassKey(AnnotatedClass::class)` for you. This contributes the class to a
`Map<Class<*>, T>`:

```kotlin
@AutoBindsIntoMap
class LoggingInterceptor @Inject constructor() : Interceptor
// --> @ClassKey(LoggingInterceptor::class) is applied automatically
```

Injection site:

```kotlin
class ApiClient @Inject constructor(
    // Since the Dagger 2 generates Java sources, you must use Java Class<*>
    // type instead of Kotlin KClass<*>:
    private val interceptors: Map<Class<*>, @JvmSuppressWildcards Interceptor>,
)
```

## Custom MapKey Annotations

Any annotation meta-annotated with `@MapKey` is recognized as a map key.
The processor detects it by checking for the `@MapKey` meta-annotation on
the annotation class:

```kotlin
// Define an enum-based map key...
enum class HandlerType { SYNC, ASYNC }

@MapKey
annotation class HandlerKey(val value: HandlerType)

// ... And use it:
@AutoBindsIntoMap
@HandlerKey(HandlerType.SYNC)
class SyncHandler @Inject constructor() : Handler

@AutoBindsIntoMap
@HandlerKey(HandlerType.ASYNC)
class AsyncHandler @Inject constructor() : Handler
```

Injection site:

```kotlin
class Dispatcher @Inject constructor(
    private val handlers: Map<HandlerType, @JvmSuppressWildcards Handler>,
)
```

## Combining with Other Annotations

`@AutoBindsIntoMap` can be combined with `@AutoBinds` and `@AutoBindsIntoSet`
on the same class. Each generates an independent module:

```kotlin
@AutoBinds          // provides Interceptor directly
@AutoBindsIntoSet   // contributes to Set<Interceptor>
@AutoBindsIntoMap   // contributes to Map (uses @ClassKey fallback)
@StringKey("default")
class DefaultInterceptor @Inject constructor() : Interceptor
```

This generates three separate modules:
- `DefaultInterceptorModule` (for `@AutoBinds`)
- `DefaultInterceptor__IntoSetModule` (for `@AutoBindsIntoSet`)
- `DefaultInterceptor__IntoMapModule` (for `@AutoBindsIntoMap`)

## Choosing a Component

`@AutoBindsIntoMap` supports the same `installIn` parameter as `@AutoBinds`.
It defaults to `HiltComponent.Unspecified` with auto-detection from scope
annotations:

```kotlin
@ActivityScoped
@AutoBindsIntoMap  // installed in ActivityComponent (auto-detected)
@StringKey("activity")
class ActivityHandler @Inject constructor() : Handler

@AutoBindsIntoMap(installIn = HiltComponent.ViewModel)
@StringKey("viewmodel")
class ViewModelHandler @Inject constructor() : Handler
```

See [Scopes and Components](scopes-and-components.md) for the full reference.

## Selecting Specific Binding Targets

By default, `@AutoBindsIntoMap` contributes the class to a `Map` of every
direct supertype. Use `bindTo` to restrict which supertypes are included or to
target a grandparent:

```kotlin
interface BaseHandler
open class AbstractHandler : BaseHandler

// Contributes to Map<String, BaseHandler> only, skipping AbstractHandler
@AutoBindsIntoMap(bindTo = [BaseHandler::class])
@StringKey("base")
class MyHandler @Inject constructor() : AbstractHandler()
```

`bindTo` accepts any transitive supertype. An error is emitted at compile time
if a listed type is not a supertype of the annotated class.

## Generated Code

For each annotated class, the processor generates an `interface` Hilt module
with `@Binds @IntoMap` functions:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingInterceptor__IntoMapModule {
    @Binds
    @IntoMap
    @StringKey("logging")
    fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
}
```

When using the `@ClassKey` fallback:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingInterceptor__IntoMapModule {
    @Binds
    @IntoMap
    @ClassKey(LoggingInterceptor::class)
    fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
}
```

The same class requirements as [basic usage](basic-usage.md#requirements-for-annotated-classes)
apply: the class must be concrete, non-abstract, not an inner class, have `@Inject` on its
primary constructor, and implement at least one interface or extend a parent class.
