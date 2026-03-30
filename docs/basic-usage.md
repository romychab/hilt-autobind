# Basic Usage

## Table of Contents

- [Single interface binding](#single-interface-binding)
- [Multiple interfaces](#multiple-interfaces)
- [Generic interfaces](#generic-interfaces)
- [Parent class binding](#parent-class-binding)
- [Selecting specific binding targets](#selecting-specific-binding-targets)
- [Qualifier annotations](#qualifier-annotations)
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

## Parent Class Binding

`@AutoBinds` also works when a class extends an `open` or `abstract` parent
class directly. A binding is generated for the parent class in the same way as
for interfaces:

```kotlin
abstract class BaseService

@AutoBinds
class HomeService @Inject constructor() : BaseService()
```

**Generated code:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface HomeServiceModule {
    @Binds
    fun bindToBaseService(impl: HomeService): BaseService
}
```

A class can mix parent class and interface supertypes - a binding is generated
for each direct supertype (excluding `Any`):

```kotlin
abstract class BaseService
interface Trackable

@AutoBinds
class HomeService @Inject constructor() : BaseService(), Trackable
```

This generates a single module with `@Binds` functions for both `BaseService`
and `Trackable`.

## Selecting Specific Binding Targets

By default, `@AutoBinds` generates a binding for every direct supertype. Use
`bindTo` to restrict which supertypes are included, or to target a
grandparent class that is not a direct supertype:

```kotlin
interface Repo
interface Closeable
interface Auditable

// Only binds to Repo; Closeable and Auditable are excluded
@AutoBinds(bindTo = [Repo::class])
class RepoImpl @Inject constructor() : Repo, Closeable, Auditable
```

```kotlin
interface GrandParent
open class Parent : GrandParent

// Binds to GrandParent even though it's not a direct supertype
@AutoBinds(bindTo = [GrandParent::class])
class Child @Inject constructor() : Parent()
```

`bindTo` accepts any transitive supertype of the annotated class. The
processor emits a compile-time error if a listed type is not a supertype
at all.

When `bindTo` is empty (the default), all direct supertypes are used.

## Qualifier Annotations

A JSR-330 qualifier annotation placed on the annotated class is forwarded to the
generated `@Binds` function, allowing Hilt to distinguish multiple bindings of the
same type:

```kotlin
@Named("prod")
@AutoBinds
class ProdApiService @Inject constructor() : ApiService

@Named("mock")
@AutoBinds
class MockApiService @Inject constructor() : ApiService
```

Custom `@Qualifier` annotations are supported in the same way. See
[Qualifiers](qualifiers.md) for the full guide, including usage with
`@AutoBindsIntoSet`, factory bindings, and annotation aliases.

## How It Works

Hilt AutoBind is a [KSP](https://github.com/google/ksp) annotation processor
that runs at compile time. For each class annotated with `@AutoBinds`, it:

1. Finds all direct supertypes (implemented interfaces and extended parent
   classes, excluding `Any`).
2. Generates an `internal interface` Hilt module with a `@Binds` function for
   each supertype.
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
- Implement **at least one interface** or extend a **non-`Any` parent class**.

The processor emits a compile-time error if any of these conditions are not met.
