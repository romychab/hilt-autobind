# Basic Usage

## Table of Contents

- [Single interface binding](#single-interface-binding)
- [Multiple interfaces](#multiple-interfaces)
- [Generic interfaces](#generic-interfaces)
- [How it works](#how-it-works)
- [Requirements for annotated classes](#requirements-for-annotated-classes)

## Single Interface Binding

Annotate a concrete class that implements an interface with `@AutoBinds`, and the
processor generates a Hilt `@Binds` module automatically.

```kotlin
interface UserRepository {
    suspend fun getUser(id: Long): User
}

@AutoBinds
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    private val db: UserDao,
) : UserRepository {

    override suspend fun getUser(id: Long): User {
        return db.getUser(id) ?: api.fetchUser(id).also { db.insert(it) }
    }
}
```

**Generated code:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface UserRepositoryImplModule {
    @Binds
    fun bindToUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

As a result, you don't need to implement Hilt binding modules manually, and Hilt can now
inject `UserRepository` anywhere.

## Multiple Interfaces

If a class implements multiple interfaces, bindings are generated for all of them:

```kotlin
@AutoBinds
class LoggerImpl @Inject constructor() : Logger, Closeable {
    override fun log(message: String) { /* ... */ }
    override fun close() { /* ... */ }
}
```

**Generated code:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggerImplModule {
    @Binds
    fun bindToLogger(impl: LoggerImpl): Logger

    @Binds
    fun bindToCloseable(impl: LoggerImpl): Closeable
}
```

## Generic Interfaces

Interfaces with type parameters are supported. The generated binding
preserves the concrete type argument:

```kotlin
interface Repository<T> {
    fun getAll(): List<T>
}

@AutoBinds
class StringRepository @Inject constructor() : Repository<String> {
    override fun getAll(): List<String> = listOf("hello")
}
```

**Generated code:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface StringRepositoryModule {
    @Binds
    fun bindToRepository(impl: StringRepository): Repository<String>
}
```

## How It Works

Hilt AutoBind is a [KSP](https://github.com/google/ksp) annotation processor
that runs at compile time. For each class annotated with `@AutoBinds`, it:

1. Finds all directly implemented interfaces.
2. Generates an `internal interface` Hilt module with a `@Binds` function for
   each interface.
3. Installs the module in the appropriate Hilt component (see
   [Scopes and Components](scopes-and-components.md)).

The generated modules are `internal`, so they don't pollute your module's
public API.

## Requirements for Annotated Classes

A class annotated with `@AutoBinds` (in default mode, without a `factory`) must:

- Be a **concrete class** (not an interface, object, or enum).
- Be **non-abstract** (no `abstract` modifier).
- **Not be an inner class** (no `inner` modifier).
- Have a **primary constructor annotated with `@Inject`**.
- Implement **at least one interface**.

The processor emits a compile-time error if any of these conditions are not met.
