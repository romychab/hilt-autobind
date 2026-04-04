package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CustomComponentResolverTest {

    @Test
    fun `installInCustomComponent generates InstallIn with custom component and scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `installInCustomComponent with unscoped component generates no scope annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject

            @DefineComponent(parent = SingletonComponent::class)
            interface MyUnscopedComponent

            interface Repo

            @AutoBinds(installInCustomComponent = MyUnscopedComponent::class)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn

            @Module
            @InstallIn(MyUnscopedComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `installInCustomComponent with matching scope on class - no conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @MyCustomScoped
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `custom scope auto-detects component via DefineComponent`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @MyCustomScoped
            @AutoBinds
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `error when custom scope has no matching DefineComponent class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class CustomScope

            interface Repo

            @CustomScope
            @AutoBinds
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("unable to resolve Hilt component for scope 'CustomScope' " +
                "on class 'RepoImpl'. No @DefineComponent class found with this scope."))
    }

    @Test
    fun `default NoCustomComponent sentinel is a no-op`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo

            @AutoBinds
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        // Default NoCustomComponent sentinel -> standard path -> SingletonComponent
        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        assertTrue(generated.contains("SingletonComponent"))
    }

    @Test
    fun `error when both installIn and installInCustomComponent are specified`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @AutoBinds(installIn = HiltComponent.Singleton, installInCustomComponent = MyCustomComponent::class)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting component specifications: use either 'installIn' " +
                "or 'installInCustomComponent', not both."))
    }

    @Test
    fun `error when installInCustomComponent and mismatching scope on class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope
            import javax.inject.Singleton

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @Singleton
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("class has conflicting scopes: [test.MyCustomScoped, " +
                "javax.inject.Singleton]. Make sure you align installIn=... param with Scope annotation."))
    }

    @Test
    fun `error when standard installIn conflicts with custom scope auto-detected`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @MyCustomScoped
            @AutoBinds(installIn = HiltComponent.Activity)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("class has conflicting scopes: " +
                "[dagger.hilt.android.scopes.ActivityScoped, test.MyCustomScoped]. " +
                "Make sure you align installIn=... param with Scope annotation."))
    }

    @Test
    fun `alias with installInCustomComponent - class has no scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            annotation class BindToMyComponent

            interface Repo

            @BindToMyComponent
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `alias with installInCustomComponent - class has matching custom scope - no conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            annotation class BindToMyComponent

            interface Repo

            @BindToMyComponent
            @MyCustomScoped
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
    }

    @Test
    fun `alias with installInCustomComponent - class has mismatching scope - error`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope
            import javax.inject.Singleton

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            annotation class BindToMyComponent

            interface Repo

            @BindToMyComponent
            @Singleton
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("class has conflicting scopes: [test.MyCustomScoped, " +
                "javax.inject.Singleton]. Make sure you align installIn=... param with Scope annotation."))
    }

    @Test
    fun `alias with installInCustomComponent - class has standard scope - error`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            annotation class BindToMyComponent

            interface Repo

            @BindToMyComponent
            @ActivityScoped
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("class has conflicting scopes: [test.MyCustomScoped, " +
                "dagger.hilt.android.scopes.ActivityScoped]. Make sure you align installIn=... param " +
                "with Scope annotation."))
    }

    @Test
    fun `alias with standard installIn - class has custom scope auto-detected - error`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installIn = HiltComponent.Singleton)
            annotation class BindToSingleton

            interface Repo

            @BindToSingleton
            @MyCustomScoped
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("class has conflicting scopes: [javax.inject.Singleton, " +
                "test.MyCustomScoped]. Make sure you align installIn=... param with Scope annotation."))
    }

    @Test
    fun `both alias and class carry custom scope for same component - ok`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            annotation class BindToMyComponent

            interface Repo

            // class has @MyCustomScoped → reverse path resolves to same MyCustomComponent → ok
            @BindToMyComponent
            @MyCustomScoped
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
    }

    @Test
    fun `alias carries custom scope annotation - class has no scope - scoped binding generated`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @MyCustomScoped
            annotation class BindToMyComponent

            interface Repo

            @BindToMyComponent
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              @MyCustomScoped
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `AutoBindsIntoSet with installInCustomComponent`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Handler

            @AutoBindsIntoSet(installInCustomComponent = MyCustomComponent::class)
            class HandlerImpl @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("HandlerImpl__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface HandlerImpl__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToHandler(`impl`: HandlerImpl): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `AutoBindsIntoMap with installInCustomComponent`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.StringKey
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Handler

            @AutoBindsIntoMap(installInCustomComponent = MyCustomComponent::class)
            @StringKey("handler")
            class HandlerImpl @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("HandlerImpl__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface HandlerImpl__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "handler")
              public fun bindToHandler(`impl`: HandlerImpl): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `installInCustomComponent combined with Named qualifier`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Named
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Repo

            @Named("custom")
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import javax.inject.Named

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              @Named(`value` = "custom")
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `alias with installInCustomComponent combined with qualifier on class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Named
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installInCustomComponent = MyCustomComponent::class)
            annotation class BindToMyComponent

            interface Repo

            @BindToMyComponent
            @Named("impl")
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import javax.inject.Named

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface RepoImplModule {
              @Binds
              @Named(`value` = "impl")
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `custom scope auto-detection with AutoBindsIntoSet`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Inject
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            interface Handler

            @MyCustomScoped
            @AutoBindsIntoSet
            class HandlerImpl @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("HandlerImpl__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface HandlerImpl__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToHandler(`impl`: HandlerImpl): Handler
            }
        """.trimIndent())
    }
}
