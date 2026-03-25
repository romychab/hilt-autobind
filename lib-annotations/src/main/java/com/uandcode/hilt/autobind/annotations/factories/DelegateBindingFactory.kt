package com.uandcode.hilt.autobind.annotations.factories

/**
 * A factory that creates an instance of [T] and exposes its public methods as
 * additional Hilt-provided dependencies.
 *
 * Implement this interface and annotate the implementation with `@Inject` to
 * use it with `@AutoBinds(factory = ...)`. The generated Hilt module will:
 * 1. Call [provideDelegate] to provide [T].
 * 2. For each public non-Unit-returning method declared on [T], generate a
 *    `@Provides` function that delegates to that method.
 *
 * The implementation must be a final, non-abstract class with no type parameters
 * (specify the concrete type on the parent interface).
 *
 * Example: Room database factory:
 * ```
 * class AppDatabaseFactory @Inject constructor(
 *     @ApplicationContext private val context: Context,
 * ) : DelegateBindingFactory<AppDatabase> {
 *     @AutoScoped
 *     override fun provideDelegate(): AppDatabase =
 *         Room.databaseBuilder(context, AppDatabase::class.java, "db").build()
 * }
 *
 * @AutoBinds(factory = AppDatabaseFactory::class)
 * @Database(entities = [NoteEntity::class], version = 1)
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun noteDao(): NoteDao
 * }
 * ```
 * This generates a module that provides both `AppDatabase` and `NoteDao`.
 */
public interface DelegateBindingFactory<T : Any> : BindingFactory {

    /**
     * Creates and returns the delegate instance of [T].
     *
     * Annotate with [AutoScoped] to scope the generated `@Provides` function.
     */
    public fun provideDelegate(): T
}
