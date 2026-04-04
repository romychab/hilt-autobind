package com.uandcode.hilt.autobind

import kotlin.reflect.KClass

/**
 * Generates a Dagger Hilt module that contributes the annotated class into a
 * multibinding `Map` of each directly implemented interface.
 *
 * The annotated class must be a concrete, non-abstract class with a primary
 * constructor annotated with `@Inject`, and must implement at least one interface.
 *
 * A map key annotation (meta-annotated with `@dagger.MapKey`) must be placed on
 * the annotated class or its alias. If no key is provided, `@ClassKey` using the
 * annotated class is used as a fallback.
 *
 * Example:
 * ```
 * @AutoBindsIntoMap
 * @StringKey("logging")
 * class LoggingInterceptor @Inject constructor() : Interceptor
 * ```
 * Generates:
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * internal interface LoggingInterceptor__IntoMapModule {
 *     @Binds @IntoMap @StringKey("logging")
 *     fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
 * }
 * ```
 *
 * Can be combined with [AutoBinds] and [AutoBindsIntoSet] on the same class to produce
 * a direct binding, a set contribution, and a map contribution simultaneously.
 *
 * @property installIn the Hilt component to install the generated module in.
 *   Defaults to [HiltComponent.Unspecified], which auto-detects the component from
 *   the scope annotation on the class (falls back to [HiltComponent.Singleton] if unscoped).
 * @property bindTo explicit list of supertypes to contribute to. When non-empty, only the listed
 *   types are used instead of all direct supertypes. Each type must be a transitive supertype
 *   of the annotated class; a compile-time error is emitted otherwise.
 * @property installInCustomComponent optional custom Hilt component class (defined with `@DefineComponent`)
 *   to install the generated module in. When set, overrides [installIn]. Setting both [installIn] and
 *   [installInCustomComponent] is a compile-time error. Defaults to [NoCustomComponent], which signals
 *   that no custom component is specified.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AutoBindsIntoMap(
    val installIn: HiltComponent = HiltComponent.Unspecified,
    val bindTo: Array<KClass<*>> = [],
    val installInCustomComponent: KClass<*> = NoCustomComponent::class,
)
