package com.uandcode.hilt.autobind

import com.uandcode.hilt.autobind.factories.BindingFactory
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import com.uandcode.hilt.autobind.factories.NoOpBindingFactory
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
 * **Delegate factory mode** - annotate a class and provide a
 * [com.uandcode.hilt.autobind.factories.DelegateBindingFactory]:
 * ```
 * @AutoBinds(factory = AppDatabaseFactory::class)
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun noteDao(): NoteDao
 * }
 * ```
 * Generates a `@Provides` module for the class itself and for each public
 * zero-argument non-Unit-returning method declared on it.
 *
 * @param installIn the Hilt component to install the generated module in.
 *   Defaults to [HiltComponent.Unspecified], which auto-detects the component from
 *   the scope annotation on the class (falls back to [HiltComponent.Singleton] if unscoped).
 * @param factory optional [BindingFactory] to use for creating instances.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class AutoBinds(
    val installIn: HiltComponent = HiltComponent.Unspecified,
    val factory: KClass<out BindingFactory> = NoOpBindingFactory::class,
)
