# Scopes and Components

## Table of Contents

- [Auto-detection (default)](#auto-detection-default)
- [Explicit component](#explicit-component)
- [Scope validation](#scope-validation)
- [Custom components](#custom-components)
- [Component and scope reference](#component-and-scope-reference)

## Auto-detection (Default)

By default, `@AutoBinds(installIn=...)` is set to `HiltComponent.Unspecified`. 
The processor auto-detects the correct component from the scope annotation on the class.
If the class has no scope annotation, `SingletonComponent` is used as the fallback.

```kotlin
// No scope annotation -> SingletonComponent
@AutoBinds
class UserRepository @Inject constructor() : UserDataSource {
    // ...
}

// @ActivityScoped -> ActivityComponent
@ActivityScoped
@AutoBinds
class SearchRepository @Inject constructor(
    private val api: SearchApi,
) : SearchDataSource {
    // ...
}
```

## Explicit Component

You can set the component explicitly via the `installIn` parameter:

```kotlin
@AutoBinds(installIn = HiltComponent.ViewModel)
class SearchRepository @Inject constructor(
    private val api: SearchApi,
) : SearchDataSource {
    // ...
}
```

This works with all `@AutoBinds`, `@AutoBindsIntoSet`, and `@AutoBindsIntoMap`
annotations, including when applied to Kotlin `object` declarations.

## Scope Validation

When both a scope annotation and an explicit `installIn` are present, the
processor validates that they match. A mismatch produces a compile error:

```kotlin
// ERROR: @Singleton does not match installIn = Activity
@Singleton
@AutoBinds(installIn = HiltComponent.Activity)
class RepoImpl @Inject constructor() : Repo
```

This prevents accidental misconfiguration where the scope and the component
would contradict each other.

## Custom Components

For custom Hilt components defined with `@DefineComponent`, see the [Custom Components](custom-components.md) page.

## Component and Scope Reference

### HiltComponent values

| `HiltComponent` value     | Dagger component            |
|---------------------------|-----------------------------|
| `Unspecified` *(default)* | auto-detected from scope    |
| `Singleton`               | `SingletonComponent`        |
| `ActivityRetained`        | `ActivityRetainedComponent` |
| `Activity`                | `ActivityComponent`         |
| `ViewModel`               | `ViewModelComponent`        |
| `Fragment`                | `FragmentComponent`         |
| `View`                    | `ViewComponent`             |
| `ViewWithFragment`        | `ViewWithFragmentComponent` |
| `Service`                 | `ServiceComponent`          |

<!-- block-start: note -->
For custom Hilt components defined with `@DefineComponent`, use `installInCustomComponent`
instead of `installIn`. The two parameters are mutually exclusive.
<!-- block-end -->

### Scope auto-detection mapping

| Scope annotation          | Resolved component          |
|---------------------------|-----------------------------|
| `@Singleton`              | `SingletonComponent`        |
| `@ActivityRetainedScoped` | `ActivityRetainedComponent` |
| `@ActivityScoped`         | `ActivityComponent`         |
| `@ViewModelScoped`        | `ViewModelComponent`        |
| `@FragmentScoped`         | `FragmentComponent`         |
| `@ViewScoped`             | `ViewComponent`             |
| `@ServiceScoped`          | `ServiceComponent`          |
| *(none)*                  | `SingletonComponent`        |

<!-- block-start: note -->
`@ViewScoped` maps to `ViewComponent` by default, not `ViewWithFragmentComponent`.
Use `installIn = HiltComponent.ViewWithFragment` explicitly if needed.
<!-- block-end -->
