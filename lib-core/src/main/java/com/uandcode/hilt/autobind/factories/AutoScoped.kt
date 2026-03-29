package com.uandcode.hilt.autobind.factories

import com.uandcode.hilt.autobind.AutoBinds

/**
 * Marks a factory method so the generated `@Provides` function receives the
 * scope annotation that corresponds to the [AutoBinds.installIn]
 * component.
 *
 * Place this on [ClassBindingFactory.create] or [DelegateBindingFactory.provideDelegate]
 * to scope the provided instance. For [DelegateBindingFactory], only the main delegate
 * is scoped; sub-delegate methods are left unscoped.
 *
 * Example:
 *
 * ```
 * class AppDatabaseFactory @Inject constructor(...) : DelegateBindingFactory<AppDatabase> {
 *     @AutoScoped
 *     override fun provideDelegate(): AppDatabase = ...
 * }
 * ```
 *
 * With `@AutoBinds(installIn = HiltComponent.Singleton)`, the generated `@Provides`
 * method will also carry `@Singleton`.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class AutoScoped
