# Optional Bindings

Dagger's `@BindsOptionalOf` lets a dependency be *optional*: consumers inject `Optional<T>` and
decide at runtime what to do when no implementation was provided. This is useful for debug-only
tools, optional integrations, and flavor-specific implementations.

Without Hilt AutoBind you write a module by hand for every optional type:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
interface DebugToolsModule {
    @BindsOptionalOf
    fun optionalDebugPanel(): DebugPanel
}
```

## Usage

Annotate the type that may or may not be provided:

```kotlin
@AutoBindsOptionalOf
interface DebugPanel {
    fun show()
}
```

The processor generates:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface DebugPanel__OptionalModule {
    @BindsOptionalOf
    public fun optionalDebugPanel(): DebugPanel
}
```

Consumers inject `Optional<DebugPanel>`:

```kotlin
import java.util.Optional

class MainViewModel @Inject constructor(
    private val debugPanel: Optional<DebugPanel>,
) : ViewModel() {

    fun openDebugPanel() {
        debugPanel.ifPresent { it.show() }
    }
}
```

If some module in the build provides a `DebugPanel`, the `Optional` is present; otherwise it is
empty. Nothing else has to change.

!!! note
    `java.util.Optional` requires minSdk 24 or core library desugaring. Dagger also accepts
    `com.google.common.base.Optional`, which works wherever Guava is on the classpath, if your
    minSdk is lower.

## Where the annotation goes

`@AutoBindsOptionalOf` is applied to the **optional type itself**: an interface or an abstract
class, not to an implementation:

```kotlin
@AutoBindsOptionalOf
interface DebugPanel        // correct

@AutoBindsOptionalOf
class DebugPanelImpl        // compile error: use @AutoBinds instead
```

The annotated type must not declare type parameters: a generic type has no single Dagger key.

`object` declarations, `enum class`, and `inner` classes are rejected for the same reason as
concrete classes: none of them describes a type that can be *optionally provided*.

## Choosing the component

Component selection works exactly as it does for [`@AutoBinds`](basic-usage.md):

```kotlin
// explicit component
@AutoBindsOptionalOf(installIn = HiltComponent.Activity)
interface DebugPanel

// custom component defined with @DefineComponent
@AutoBindsOptionalOf(installInCustomComponent = MyCustomComponent::class)
interface DebugPanel

// component inferred from the scope annotation
@AutoBindsOptionalOf
@ActivityScoped
interface DebugPanel
```

Without any of these, the module is installed in `SingletonComponent`.

!!! note
    A scope annotation only *selects* the component. Dagger forbids scoped `@BindsOptionalOf`
    declarations, so the generated function never carries a scope annotation.

## Qualifiers

Qualifiers on the type are forwarded to the generated declaration, so you can make a qualified
dependency optional:

```kotlin
@AutoBindsOptionalOf
@Named("debug")
interface DebugPanel
```

Consumers then inject `@Named("debug") Optional<DebugPanel>`.

## Annotation aliases

`@AutoBindsOptionalOf` works as a meta-annotation, including across modules, see
[Annotation Aliases](annotation-aliases.md):

```kotlin
@AutoBindsOptionalOf(installIn = HiltComponent.Activity)
@Target(AnnotationTarget.CLASS)
annotation class ActivityOptional

@ActivityOptional
interface DebugPanel
```

## Combining with `@AutoBinds`

Declaring a type optional does not prevent it from being provided. A type can be declared optional
in one module and bound with `@AutoBinds` in another - consumers simply receive a present
`Optional`. This is the point of the feature: the consuming code does not need to know which build
variants supply an implementation.
