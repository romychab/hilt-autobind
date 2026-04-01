# Hilt AutoBind

[![Maven Central](https://img.shields.io/maven-central/v/com.uandcode/hilt-autobind.svg?label=Maven%20Central)](https://central.sonatype.com/search?q=g:com.uandcode+a:hilt-autobind-*)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![PR Check](https://github.com/romychab/hilt-autobind/actions/workflows/pr-check.yml/badge.svg)](https://github.com/romychab/hilt-autobind/actions/workflows/pr-check.yml)
[![Coverage](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/romychab/0934de7a7449df001be47fa0c6286b33/raw/hilt-autobind-coverage.json)](https://github.com/romychab/hilt-autobind/actions/workflows/publish.yml)
[![Publish](https://github.com/romychab/hilt-autobind/actions/workflows/publish.yml/badge.svg)](https://github.com/romychab/hilt-autobind/actions/workflows/publish.yml)

Auto-generate Dagger Hilt binding modules with a single annotation. Stop writing
repetitive `@Module` / `@Binds` / `@Provides` boilerplate.

<!-- docs-exclude-start -->
## Documentation

Full documentation is available [here](https://docs.uandcode.com/hilt-autobind/).
<!-- docs-exclude-end -->

## Quick Start

Add dependencies to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.uandcode:hilt-autobind:0.6.0")
    ksp("com.uandcode:hilt-autobind-compiler:0.6.0")
}
```

> Requires [Hilt](https://dagger.dev/hilt/) and [KSP](https://github.com/google/ksp).
> See [Installation](docs/installation.md) for full setup instructions.

Annotate your implementation class:

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

The KSP processor generates a Hilt `@Binds` module for you:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
internal interface UserRepositoryImplModule {
    @Binds
    fun bindToUserRepository(impl: UserRepositoryImpl): UserRepository
}
```

Manual modules are not needed anymore, and Hilt can now inject `UserRepository` anywhere.

## Features

| Feature                                              | Annotation / parameter          | Description                                                                         |
|------------------------------------------------------|---------------------------------|-------------------------------------------------------------------------------------|
| [Basic binding](docs/basic-usage.md)                 | `@AutoBinds`                    | Generates `@Binds` modules for interface implementations                            |
| [Scopes & components](docs/scopes-and-components.md) | `installIn`                     | Auto-detects or explicitly sets the Hilt component                                  |
| [Class factory](docs/class-factory.md)               | `@AutoBinds(factory = ...)`     | Delegates instance creation to a `ClassBindingFactory` (e.g., Retrofit)             |
| [Delegate factory](docs/delegate-factory.md)         | `@AutoBinds(factory = ...)`     | Provides a class and its sub-dependencies via `DelegateBindingFactory` (e.g., Room) |
| [Set multibinding](docs/multibinding.md)             | `@AutoBindsIntoSet`             | Contributes to a Dagger `Set` multibinding                                          |
| [Map multibinding](docs/multibinding-map.md)         | `@AutoBindsIntoMap`             | Contributes to a Dagger `Map` multibinding with a `@MapKey`-annotated key           |
| [Qualifiers](docs/qualifiers.md)                     | `@Named` / custom `@Qualifier`  | Distinguishes multiple bindings of the same type with JSR-330 qualifier annotations |
| [Annotation aliases](docs/annotation-aliases.md)     | `@AutoBinds` as meta-annotation | Define custom annotations that act as short aliases for `@AutoBinds`                |
| [Multi-Module Projects](docs/multi-module.md)        | -                               | Per-module setup in multi-module projects                                           |

## Requirements

| Dependency            | Minimum version |
|-----------------------|-----------------|
| Kotlin                | 2.0+            |
| KSP                   | 2.0+            |
| Dagger Hilt           | 2.50+           |
| Android Gradle Plugin | 8.0+            |
