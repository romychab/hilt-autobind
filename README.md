# Hilt AutoBind

[![Maven Central](https://img.shields.io/maven-central/v/com.uandcode/hilt-autobind-annotations.svg)](https://central.sonatype.com/search?q=g:com.uandcode+a:hilt-autobind-*)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

Auto-generate Dagger Hilt binding modules with a single annotation. Stop writing
repetitive `@Module` / `@Binds` / `@Provides` boilerplate. Just annotate your
class with `@AutoBinds` and let the KSP compiler plugin do the rest.

## Setup

Add the dependencies to your module's `build.gradle.kts`:

```kotlin
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>"
    // ... other plugins
}

dependencies {
    implementation("com.uandcode:hilt-autobind:0.1.0")
    ksp("com.uandcode:hilt-autobind-compiler:0.1.0")
}
```

> The library requires [Dagger Hilt](https://dagger.dev/hilt/) and
> [KSP](https://github.com/google/ksp) to be set up in your project.

## Usage

### Basic binding (Default mode)

Annotate a concrete class that implements an interface. The processor generates
a Hilt `@Binds` module automatically.

```kotlin
// 1. Define your interface
interface UserRepository {
    suspend fun getUser(id: Long): User
}

// 2. Annotate the implementation, that's it!
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

No manual module needed. Hilt can now inject `UserRepository` anywhere.

### Multiple interfaces

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

### Choosing a Hilt component

By default, `installIn` is set to `HiltComponent.Unspecified`. The processor
auto-detects the correct component from the scope annotation on the class.
If the class has no scope annotation, `SingletonComponent` is used as the
fallback.

```kotlin
// Auto-detected: @ActivityScoped -> ActivityComponent
@ActivityScoped
@AutoBinds
class SearchRepository @Inject constructor(
    private val api: SearchApi,
) : SearchDataSource {
    // ...
}
```

You can also set the component explicitly:

```kotlin
@AutoBinds(installIn = HiltComponent.ViewModel)
class SearchRepository @Inject constructor(
    private val api: SearchApi,
) : SearchDataSource {
    // ...
}
```

When both a scope annotation and an explicit `installIn` are present, the
processor validates that they match and emits a compile error on mismatch.

All standard Hilt components are supported:

| `HiltComponent` value       | Dagger component             |
|-----------------------------|------------------------------|
| `Unspecified` *(default)*   | auto-detected from scope     |
| `Singleton`                 | `SingletonComponent`         |
| `ActivityRetained`          | `ActivityRetainedComponent`  |
| `Activity`                  | `ActivityComponent`          |
| `ViewModel`                 | `ViewModelComponent`         |
| `Fragment`                  | `FragmentComponent`          |
| `View`                      | `ViewComponent`              |
| `ViewWithFragment`          | `ViewWithFragmentComponent`  |
| `Service`                   | `ServiceComponent`           |

### Class factory mode

For cases where you don't control instance creation (e.g., Retrofit interfaces),
use the `factory` parameter with a `ClassBindingFactory` implementation.

**Step 1.** Create a factory:

```kotlin
@Singleton
class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {

    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }

}
```

**Step 2.** Annotate interfaces with `@AutoBinds(factory = ...)`:

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

**Generated code** (for each interface):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object UserApiModule {
    @Provides
    fun provideUserApi(factory: RetrofitBindingFactory): UserApi =
        factory.create(UserApi::class)
}
```

Each Retrofit interface gets its own module. No manual `@Provides` functions needed.

### Delegate factory mode

When a class exposes multiple dependencies through its methods (e.g., a Room
database with DAO accessors), use `DelegateBindingFactory`. The processor
generates `@Provides` functions for both the class itself and every public
non-Unit-returning method declared on it.

**Step 1.** Create a factory:

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

**Step 2.** Annotate the class with `@AutoBinds(factory = ...)`:

```kotlin
@AutoBinds(factory = AppDatabaseFactory::class)
@Database(entities = [NoteEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun orderDao(): OrderDao
}
```

**Generated code:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal object AppDatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(factory: AppDatabaseFactory): AppDatabase =
        factory.provideDelegate()

    @Provides
    fun provideNoteDao(delegate: AppDatabase): NoteDao = delegate.noteDao()

    @Provides
    fun provideOrderDao(delegate: AppDatabase): OrderDao = delegate.orderDao()
}
```

Both `AppDatabase` and all its DAOs become injectable with a single annotation.

The `DelegateBindingFactory` subclass must be:
- A final, non-abstract, non-object class (no `open` or `abstract` modifier)
- Free of type parameters (specify the concrete type on the parent, e.g.,
  `DelegateBindingFactory<AppDatabase>`)
- Have a primary constructor annotated with `@Inject`

### `@AutoScoped`

By default, generated `@Provides` functions are unscoped. Annotate the factory
method with `@AutoScoped` to add the scope annotation that matches the
`installIn` component:

| `installIn`        | Scope added by `@AutoScoped`  |
|--------------------|-------------------------------|
| `Singleton`        | `@Singleton`                  |
| `ActivityRetained` | `@ActivityRetainedScoped`     |
| `Activity`         | `@ActivityScoped`             |
| `ViewModel`        | `@ViewModelScoped`            |
| `Fragment`         | `@FragmentScoped`             |
| `View`             | `@ViewScoped`                 |
| `ViewWithFragment` | `@ViewScoped`                 |
| `Service`          | `@ServiceScoped`              |

`@AutoScoped` can be placed on:
- `ClassBindingFactory.create()`: scopes the generated `@Provides` for every
  class that uses this factory.
- `DelegateBindingFactory.provideDelegate()`: scopes only the main delegate;
  sub-delegate methods remain unscoped.

### Multibinding with `@AutoBindsIntoSet`

Use `@AutoBindsIntoSet` to contribute a class into a Dagger multibinding `Set`
of each directly implemented interface:

```kotlin
@AutoBindsIntoSet
class LoggingInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Chain): Response { /* ... */ }
}

@AutoBindsIntoSet
class AuthInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Chain): Response { /* ... */ }
}
```

**Generated code** (for each class):

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface LoggingInterceptor__IntoSetModule {
    @Binds @IntoSet
    fun bindToInterceptor(impl: LoggingInterceptor): Interceptor
}
```

Now you can inject `Set<Interceptor>` anywhere and Hilt will collect all
contributions automatically.

`@AutoBindsIntoSet` supports the same `installIn` parameter as `@AutoBinds`
(defaults to `Unspecified` with auto-detection). It can be combined with
`@AutoBinds` on the same class to produce both a direct binding and a set
contribution:

```kotlin
@AutoBinds            // provides Interceptor directly
@AutoBindsIntoSet     // also contributes to Set<Interceptor>
class DefaultInterceptor @Inject constructor() : Interceptor { /* ... */ }
```

## How it works

Hilt AutoBind is a [KSP](https://github.com/google/ksp) annotation processor
that runs at compile time. It:

1. Finds all classes/interfaces annotated with `@AutoBinds` or `@AutoBindsIntoSet`.
2. For **default mode** (no factory): generates an `interface` Hilt module with
   `@Binds` functions for each directly implemented interface.
3. For **class factory mode**: generates an `object` Hilt module with a
   `@Provides` function that delegates to the specified `ClassBindingFactory`.
4. For **delegate factory mode**: generates an `object` Hilt module with a
   `@Provides` function for the delegate and additional `@Provides` functions
   for each public non-Unit-returning method declared on the annotated class.
5. For **multibinding** (`@AutoBindsIntoSet`): generates an `interface` Hilt
   module with `@Binds @IntoSet` functions.

The generated modules are `internal`, so they don't pollute your module's public API.

## Requirements

| Dependency              | Minimum version |
|-------------------------|-----------------|
| Kotlin                  | 2.0+            |
| KSP                     | 2.0+            |
| Dagger Hilt             | 2.50+           |
| Android Gradle Plugin   | 8.0+            |
