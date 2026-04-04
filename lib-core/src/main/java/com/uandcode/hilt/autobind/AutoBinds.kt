package com.uandcode.hilt.autobind

import com.uandcode.hilt.autobind.factories.BindingFactory
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import com.uandcode.hilt.autobind.factories.NoOpBindingFactory
import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
import kotlin.reflect.KClass

/**
 * Automatically generates a Dagger Hilt module for the annotated class.
 *
 * **Default mode** - annotate a concrete class with `@Inject` constructor:
 * ```
 * @AutoBinds
 * class RepositoryImpl @Inject constructor(...) : Repository
 * ```
 * Generates a `@Binds` module binding `RepositoryImpl` to `Repository`.
 *
 * **Class factory mode** - annotate an interface and provide a [ClassBindingFactory]:
 * ```
 * @AutoBinds(factory = RetrofitBindingFactory::class)
 * interface MyApi { ... }
 * ```
 * Generates a `@Provides` module that delegates instance creation to the factory.
 *
 * **Delegate factory mode** - annotate a class and provide a [DelegateBindingFactory]:
 * ```
 * @AutoBinds(factory = AppDatabaseFactory::class)
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun noteDao(): NoteDao
 * }
 * ```
 * Generates a `@Provides` module for the class itself and for each public
 * zero-argument non-Unit-returning method declared on it.
 *
 * @property installIn the Hilt component to install the generated module in.
 *   Defaults to [HiltComponent.Unspecified], which auto-detects the component from
 *   the scope annotation on the class (falls back to [HiltComponent.Singleton] if unscoped).
 * @property factory optional [BindingFactory] to use for creating instances.
 * @property bindTo explicit list of supertypes to bind to. When non-empty, only the listed
 *   types are used as binding targets instead of all direct supertypes. Each type must be
 *   a transitive supertype of the annotated class; a compile-time error is emitted otherwise.
 *   Supports grandparent classes and interfaces that are not direct supertypes.
 * @property installInCustomComponent optional custom Hilt component class (defined with `@DefineComponent`)
 *   to install the generated module in. When set, overrides [installIn]. Setting both [installIn] and
 *   [installInCustomComponent] is a compile-time error. Defaults to [NoCustomComponent], which signals
 *   that no custom component is specified.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AutoBinds(
    val installIn: HiltComponent = HiltComponent.Unspecified,
    val factory: KClass<out BindingFactory> = NoOpBindingFactory::class,
    val bindTo: Array<KClass<*>> = [],
    val installInCustomComponent: KClass<*> = NoCustomComponent::class,
)
