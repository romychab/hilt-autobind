package com.uandcode.hilt.autobind

import kotlin.reflect.KClass

/**
 * Generates a Dagger Hilt module that declares the annotated type as an optional
 * dependency via Dagger's `@BindsOptionalOf`.
 *
 * Apply it to the interface or abstract class that may or may not be provided:
 * ```
 * @AutoBindsOptionalOf
 * interface DebugPanel {
 *     fun show()
 * }
 * ```
 * Generates:
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * internal interface DebugPanel__OptionalModule {
 *     @BindsOptionalOf
 *     public fun optionalDebugPanel(): DebugPanel
 * }
 * ```
 *
 * Consumers can then inject `Optional<DebugPanel>` and check at runtime whether an
 * implementation was provided.
 *
 * The annotated type must be an interface or an abstract class and must not declare
 * type parameters.
 *
 * @property installIn the Hilt component to install the generated module in.
 *   Defaults to [HiltComponent.Unspecified], which auto-detects the component from
 *   the scope annotation on the annotated type (falls back to [HiltComponent.Singleton]
 *   if unscoped). The scope only selects the component: Dagger forbids scoped
 *   `@BindsOptionalOf` declarations, so no scope annotation is ever emitted.
 * @property installInCustomComponent optional custom Hilt component class (defined with `@DefineComponent`)
 *   to install the generated module in. When set, overrides [installIn]. Setting both [installIn] and
 *   [installInCustomComponent] is a compile-time error. Defaults to [NoCustomComponent], which signals
 *   that no custom component is specified.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AutoBindsOptionalOf(
    val installIn: HiltComponent = HiltComponent.Unspecified,
    val installInCustomComponent: KClass<*> = NoCustomComponent::class,
)
