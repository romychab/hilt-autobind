# Qualifiers

## Table of Contents

- [Overview](#overview)
- [Using @Named](#using-named-qualifier)
- [Custom qualifier annotations](#custom-qualifier-annotations)
- [Qualifiers with Multibinding](#qualifiers-with-multibinding)
  - [Set multibinding](#set-multibinding)
  - [Map multibinding](#map-multibinding)
- [Qualifiers with factory bindings](#qualifiers-with-factory-bindings)
- [Qualifiers on annotation aliases](#qualifiers-on-annotation-aliases)
- [Qualifiers combined with scopes](#qualifiers-combined-with-scopes)
- [Qualifier conflict rules](#qualifier-conflict-rules)

## Overview

Qualifiers are JSR-330 annotations that distinguish multiple bindings of the same type.
When two classes implement the same interface but should be injected in different contexts,
a qualifier annotation tells Hilt (and Dagger) which binding to use.

The annotation processor forwards any qualifier annotation found on the annotated class
or object (or its alias) directly to the generated binding function. No extra configuration
is needed.

## Using `@Named` qualifier

`@Named` is the most commonly used built-in qualifier from `javax.inject`:

```kotlin
interface ApiService

@Named("prod")
@AutoBinds
class ProdApiService @Inject constructor() : ApiService

@Named("mock")
@AutoBinds
class MockApiService @Inject constructor() : ApiService
```

Generated modules:

```kotlin
// ProdApiServiceModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal interface ProdApiServiceModule {
    @Binds
    @Named("prod") // <-- qualifier is added here
    fun bindToApiService(impl: ProdApiService): ApiService
}

// MockApiServiceModule.kt
@Module
@InstallIn(SingletonComponent::class)
internal interface MockApiServiceModule {
    @Binds
    @Named("mock") // <-- qualifier is added here
    fun bindToApiService(impl: MockApiService): ApiService
}
```

Injection site:

```kotlin
class MyViewModel @Inject constructor(
    @Named("prod") private val api: ApiService,
)
```

## Custom Qualifier Annotations

For production code, prefer custom qualifier annotations over `@Named` strings.
They are type-safe and refactor-friendly:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ProdApi

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MockApi
```

Apply them the same way as `@Named` qualifier:

```kotlin
@ProdApi
@AutoBinds
class ProdApiService @Inject constructor() : ApiService

@MockApi
@AutoBinds
class MockApiService @Inject constructor() : ApiService
```

Example of generated Hilt module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface ProdApiServiceModule {
    @Binds
    @ProdApi // <-- qualifier is added here
    fun bindToApiService(impl: ProdApiService): ApiService
}
```

Injection site:

```kotlin
class MyViewModel @Inject constructor(
    @ProdApi private val api: ApiService,
)
```

## Qualifiers with Multibinding

### Set multibinding

Qualifiers work with `@AutoBindsIntoSet` in the same way:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DebugInterceptors

@DebugInterceptors
@AutoBindsIntoSet
class LoggingInterceptor @Inject constructor() : Interceptor

@DebugInterceptors
@AutoBindsIntoSet
class AuthInterceptor @Inject constructor() : Interceptor
```

Generated:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingInterceptor__IntoSetModule {
    @Binds
    @IntoSet
    @DebugInterceptors
    fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
}
```

Injection site:

```kotlin
class OkHttpClientFactory @Inject constructor(
    @DebugInterceptors private val interceptors: Set<Interceptor>,
)
```

### Map multibinding

Qualifiers work with `@AutoBindsIntoMap` the same way. The qualifier is placed
on the `@Binds @IntoMap` function alongside the map key annotation:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DebugHandlers

@DebugHandlers
@AutoBindsIntoMap
@StringKey("logging")
class LoggingHandler @Inject constructor() : Handler
```

Generated:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingHandler__IntoMapModule {
    @Binds
    @IntoMap
    @StringKey("logging")
    @DebugHandlers
    fun bindToHandler(impl: LoggingHandler): Handler
}
```

Injection site:

```kotlin
class HandlerRegistry @Inject constructor(
    @DebugHandlers private val handlers: Map<String, @JvmSuppressWildcards Handler>,
)
```

## Qualifiers with Factory Bindings

Qualifiers are also forwarded to auto-generated modules when
using `ClassBindingFactory` or `DelegateBindingFactory`.

### ClassBindingFactory

```kotlin

class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {
    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }
}

@Named("retro") // <-- the qualifier to be forwarded 
@AutoBinds(factory = RetrofitFactory::class)
interface UserApi
```

Generated Hilt module:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object UserApiV2Module {
    @Provides
    @Singleton
    @Named("retro") // <-- forwarded qualifier
    fun provideUserApiV2(factory: RetrofitFactory): UserApiV2 = factory.create(UserApiV2::class)
}
```

### DelegateBindingFactory

The qualifier is applied to the primary `@Provides` function that creates the delegate
instance. Sub-dependency `@Provides` functions (for abstract methods of the delegated
class) do not carry the qualifier:

```kotlin
@Named("main")
@AutoBinds(factory = AppDatabaseFactory::class)
abstract class AppDatabase {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
}
```

Generated:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object AppDatabaseModule {
    @Provides
    @Named("main")
    fun provideAppDatabase(factory: AppDatabaseFactory): AppDatabase =
        factory.provideDelegate()

    @Provides
    fun provideUserDao(
        @Named("main") delegate: AppDatabase,
    ): UserDao = delegate.userDao()

    @Provides
    fun providePostDao(
        @Named("main") delegate: AppDatabase,
    ): PostDao = delegate.postDao()
}
```

## Qualifiers on Annotation Aliases

Qualifier annotations can be baked into an alias so every class using that alias
automatically gets the qualifier:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RetrofitApi

@Target(AnnotationTarget.CLASS)
@AutoBinds(factory = RetrofitFactory::class)
@RetrofitApi
annotation class BindRetrofit
```

Applying the alias:

```kotlin
@BindRetrofit
interface UserApi { /* ... */ }

@BindRetrofit
interface PostApi { /* ... */ }
```

Both generate `@Provides @RetrofitApi fun provide...` without any per-class qualifier
annotation.

Injection site:

```kotlin
class SomeClass @Inject constructor(
    @RetrofitApi private val userApi: UserApi,
    @RetrofitApi private val postApi: PostApi,
)
```

## Qualifiers Combined with Scopes

Qualifiers and scope annotations can be used together. When the scope comes from an
alias, the processor forwards both the scope and the qualifier to the generated function:

```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class MainRepo

@Target(AnnotationTarget.CLASS)
@AutoBinds
@Singleton
@MainRepo
annotation class BindMainSingleton

@BindMainSingleton
class UserRepositoryImpl @Inject constructor() : UserRepository
```

Generated:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface UserRepositoryImplModule {
    @Binds
    @Singleton
    @MainRepo
    fun bindToUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

## Qualifier Conflict Rules

- A class may carry **at most one** qualifier annotation (directly or via alias).
- An alias may carry **at most one** qualifier annotation.
- If the class and its alias both carry **the same qualifier type**, no conflict is
  reported and the qualifier is applied once.
- If the class and its alias carry **different qualifier types**, the processor emits
  a compile-time error.
