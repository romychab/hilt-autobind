# Delegate Factory

## Table of Contents

- [Overview](#overview)
- [Creating a factory](#creating-a-factory)
- [Annotating the class](#annotating-the-class)
- [Scoping with @AutoScoped](#scoping-with-autoscoped)
- [Generated code](#generated-code)
- [Factory requirements](#factory-requirements)

## Overview

When a class exposes multiple dependencies through its methods (e.g., a Room
database with DAO accessors), use `DelegateBindingFactory`. The processor
generates `@Provides` functions for both the class itself and every public
zero-argument non-Unit-returning method declared on it.

## Creating a Factory

Implement `DelegateBindingFactory<T>` where `T` is the type of the delegate
class. The `provideDelegate()` method creates and returns the delegate instance:

```kotlin
class AppDatabaseFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DelegateBindingFactory<AppDatabase> {

    @AutoScoped
    override fun provideDelegate(): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database",
        ).build()
    }
}
```

## Annotating the Class

Point the class to the factory using `@AutoBinds(factory = ...)`:

```kotlin
@AutoBinds(factory = AppDatabaseFactory::class)
@Database(entities = [NoteEntity::class, OrderEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun orderDao(): OrderDao
}
```

## Scoping with @AutoScoped

Annotate `provideDelegate()` with `@AutoScoped` to scope the main delegate
provider. Sub-delegate methods (like `noteDao()`) remain **unscoped** and are
re-resolved from the delegate on each injection:

```kotlin
class AppDatabaseFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DelegateBindingFactory<AppDatabase> {

    @AutoScoped  // scopes the returned AppDatabase instance
    override fun provideDelegate(): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database",
        ).build()
    }
}
```

The scope annotation is determined by the resolved component. See
[Scopes and Components](scopes-and-components.md) for the full mapping table.

## Generated Code

The processor generates an `object` Hilt module with:

- A `@Provides` function for the delegate itself (via `provideDelegate()`).
- A `@Provides` function for each public **zero-argument** non-Unit-returning
  method declared directly on the annotated class.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object AppDatabaseModule {
    @Provides
    @Singleton  // present only if @AutoScoped is used
    fun provideAppDatabase(factory: AppDatabaseFactory): AppDatabase =
        factory.provideDelegate()

    @Provides
    fun provideNoteDao(delegate: AppDatabase): NoteDao = delegate.noteDao()

    @Provides
    fun provideOrderDao(delegate: AppDatabase): OrderDao = delegate.orderDao()
}
```

Both `AppDatabase` and all its DAOs become injectable with a single annotation.

## Factory Requirements

A `DelegateBindingFactory` subclass must be:

- A **final class** (no `open` or `abstract` modifier, not an `object`).
- Free of **type parameters** (specify the concrete type on the parent, e.g.,
  `DelegateBindingFactory<AppDatabase>`).
- Have a **primary constructor annotated with `@Inject`**.
- **Directly implement** `DelegateBindingFactory` (not through an intermediate
  class).

The processor emits a compile-time error if any of these conditions are not met.
