package com.uandcode.hilt.autobind.annotations

/**
 * Generates a Dagger Hilt module that contributes the annotated class into a
 * multibinding `Set` of each directly implemented interface.
 *
 * The annotated class must be a concrete, non-abstract class with a primary
 * constructor annotated with `@Inject`, and must implement at least one interface.
 *
 * Example:
 * ```
 * @AutoBindsIntoSet
 * class LoggingInterceptor @Inject constructor() : Interceptor
 * ```
 * Generates:
 * ```
 * @Module
 * @InstallIn(SingletonComponent::class)
 * internal interface LoggingInterceptor__IntoSetModule {
 *     @Binds @IntoSet
 *     fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
 * }
 * ```
 *
 * Can be combined with [AutoBinds] on the same class to produce both a direct
 * binding and a set contribution.
 *
 * @param installIn the Hilt component to install the generated module in.
 *   Defaults to [HiltComponent.Unspecified], which auto-detects the component from
 *   the scope annotation on the class (falls back to [HiltComponent.Singleton] if unscoped).
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
public annotation class AutoBindsIntoSet(
    val installIn: HiltComponent = HiltComponent.Unspecified,
)
