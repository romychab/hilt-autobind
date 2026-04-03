# Class Factory

## Table of Contents

- [Overview](#overview)
- [Creating a factory](#creating-a-factory)
- [Annotating interfaces](#annotating-interfaces)
- [Scoping with @AutoScoped](#scoping-with-autoscoped)
- [Generated code](#generated-code)

## Overview

For cases where you don't control instance creation (e.g., Retrofit interfaces),
use the `factory` parameter with a `ClassBindingFactory` implementation. The
processor generates `@Provides` functions that delegate to your factory.

## Creating a Factory

Implement the `ClassBindingFactory` interface. The `create` method receives a
`KClass` and returns an instance of that type:

```kotlin
class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {

    @AutoScoped // <-- optional
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }
}
```

The factory class must have a primary constructor annotated with `@Inject`.

## Annotating Interfaces

Point each interface to the factory using `@AutoBinds(factory = ...)`:

```kotlin
@AutoBinds(factory = RetrofitBindingFactory::class)
interface UserApi {
    @GET("users/{id}")
    suspend fun fetchUser(@Path("id") id: Long): UserDto
}

@AutoBinds(factory = RetrofitBindingFactory::class)
interface PostApi {
    @GET("posts")
    suspend fun getPosts(): List<PostDto>
}
```

Each interface gets its own generated module, so you don't need to write `@Provides`
functions manually.

## Scoping with @AutoScoped

By default, generated `@Provides` functions are **unscoped**. Annotate the
factory's `create()` method with `@AutoScoped` to add a scope annotation that
matches the `installIn` component:

```kotlin
class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {

    @AutoScoped  // adds @Singleton (or the scope matching installIn)
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }
}
```

The scope annotation is determined by the resolved component:

| `installIn`        | Scope added by `@AutoScoped` |
|--------------------|------------------------------|
| `Singleton`        | `@Singleton`                 |
| `ActivityRetained` | `@ActivityRetainedScoped`    |
| `Activity`         | `@ActivityScoped`            |
| `ViewModel`        | `@ViewModelScoped`           |
| `Fragment`         | `@FragmentScoped`            |
| `View`             | `@ViewScoped`                |
| `ViewWithFragment` | `@ViewScoped`                |
| `Service`          | `@ServiceScoped`             |

## Generated Code

For each annotated interface, the processor generates an `object` Hilt module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object UserApiModule {
    @Provides
    @Singleton  // present only if @AutoScoped is used
    fun provideUserApi(factory: RetrofitBindingFactory): UserApi =
        factory.create(UserApi::class)
}
```
