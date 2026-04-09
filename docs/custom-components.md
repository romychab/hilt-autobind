# Custom Components

Dagger Hilt supports defining custom components with `@DefineComponent`. The `installInCustomComponent`
parameter accepts a reference to such a component class, allowing the generated module to be installed there.

For a general guide on custom Hilt components, see the
[official Hilt documentation](https://dagger.dev/hilt/custom-components.html).

## Defining a custom component

```kotlin
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class MyCustomScoped

@MyCustomScoped
@DefineComponent(parent = SingletonComponent::class)
interface MyCustomComponent
```

## Explicit component

Pass the component class directly with `installInCustomComponent`:

```kotlin
@AutoBinds(installInCustomComponent = MyCustomComponent::class)
class RepositoryImpl @Inject constructor() : Repository
```

The processor reads the `@Scope`-annotated annotation from `MyCustomComponent` automatically,
no need to repeat `@MyCustomScoped` on the implementation class unless you want the class to be scoped.

## Scope auto-detection

Place the custom scope annotation on the class and omit `installInCustomComponent`. The processor
finds the `@DefineComponent` class annotated with that scope automatically:

```kotlin
@MyCustomScoped
@AutoBinds
class RepositoryImpl @Inject constructor() : Repository
// --> @InstallIn(MyCustomComponent::class) generated automatically
```

## Validation rules

- `installIn` and `installInCustomComponent` cannot be used together -> compile error.
- A scope annotation on the class that does not match the resolved component -> compile error.
- A custom scope annotation with no matching `@DefineComponent` class -> compile error with a hint
  to use `installInCustomComponent` explicitly.
- All other features (qualifiers, `bindTo`, annotation aliases, `@AutoBindsIntoSet`,
  `@AutoBindsIntoMap`, Kotlin `object` declarations) work with custom components
  identically to standard Hilt components.
