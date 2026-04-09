package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObjectAsTargetTest {

    @Test
    fun `generates Provides module for simple interface`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            interface UserRepository

            @AutoBinds
            object UserRepositoryImpl : UserRepository
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("UserRepositoryImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object UserRepositoryImplModule {
              @Provides
              public fun bindToUserRepository(): UserRepository = UserRepositoryImpl
            }
        """.trimIndent())
    }

    @Test
    fun `generates Provides module for multiple interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            interface Logger
            interface Closeable

            @AutoBinds
            object LoggerImpl : Logger, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggerImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggerImplModule {
              @Provides
              public fun bindToLogger(): Logger = LoggerImpl

              @Provides
              public fun bindToCloseable(): Closeable = LoggerImpl
            }
        """.trimIndent())
    }

    @Test
    fun `uses explicit installIn Activity component`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent

            interface Repo

            @AutoBinds(installIn = HiltComponent.Activity)
            object RepoImpl : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal object RepoImplModule {
              @Provides
              public fun bindToRepo(): Repo = RepoImpl
            }
        """.trimIndent())
    }

    @Test
    fun `bindTo restricts binding targets`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            interface Logger
            interface Closeable

            @AutoBinds(bindTo = [Logger::class])
            object LoggerImpl : Logger, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggerImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggerImplModule {
              @Provides
              public fun bindToLogger(): Logger = LoggerImpl
            }
        """.trimIndent())
    }

    @Test
    fun `Named qualifier is forwarded to generated Provides function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Named

            interface Repo

            @Named("main")
            @AutoBinds
            object RepoImpl : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal object RepoImplModule {
              @Provides
              @Named(`value` = "main")
              public fun bindToRepo(): Repo = RepoImpl
            }
        """.trimIndent())
    }

    @Test
    fun `scope annotation on alias auto-detects component and forwards scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityScoped

            interface Repo

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @ActivityScoped
            annotation class ContributeActivityRepo

            @ContributeActivityRepo
            object RepoImpl : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.hilt.android.scopes.ActivityScoped

            @Module
            @InstallIn(ActivityComponent::class)
            internal object RepoImplModule {
              @Provides
              @ActivityScoped
              public fun bindToRepo(): Repo = RepoImpl
            }
        """.trimIndent())
    }

    @Test
    fun `error when object has no interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            @AutoBinds
            object Standalone
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must implement at least one interface or extend a super-class"
        ))
    }

    @Test
    fun `generates Provides module for parent class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            open class ParentClass

            @AutoBinds
            object ChildObject : ParentClass()
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ChildObjectModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object ChildObjectModule {
              @Provides
              public fun bindToParentClass(): ParentClass = ChildObject
            }
        """.trimIndent())
    }

    @Test
    fun `generates Provides IntoSet module`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet

            interface Interceptor

            @AutoBindsIntoSet
            object LoggingInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptor__IntoSetModule {
              @Provides
              @IntoSet
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())
    }

    @Test
    fun `Named qualifier is forwarded to generated Provides IntoSet function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Named

            interface Interceptor

            @Named("logging")
            @AutoBindsIntoSet
            object LoggingInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptor__IntoSetModule {
              @Provides
              @Named(`value` = "logging")
              @IntoSet
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())
    }

    @Test
    fun `error when object annotated with AutoBindsIntoSet has no interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet

            @AutoBindsIntoSet
            object Standalone
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must implement at least one interface or extend a super-class"
        ))
    }

    @Test
    fun `generates Provides IntoMap module with StringKey`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey

            interface Interceptor

            @AutoBindsIntoMap
            @StringKey("logging")
            object LoggingInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptor__IntoMapModule {
              @Provides
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())
    }

    @Test
    fun `Named qualifier is forwarded to generated Provides IntoMap function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Named

            interface Interceptor

            @Named("debug")
            @AutoBindsIntoMap
            @StringKey("logging")
            object LoggingInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptor__IntoMapModule {
              @Provides
              @Named(`value` = "debug")
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())
    }

    @Test
    fun `AutoBindsIntoMap scope annotation on alias auto-detects component and forwards scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.multibindings.StringKey

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @ActivityScoped
            annotation class ContributeActivityScopedHandler

            @ContributeActivityScopedHandler
            @StringKey("main")
            object MainHandler : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainHandler__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(ActivityComponent::class)
            internal object MainHandler__IntoMapModule {
              @Provides
              @ActivityScoped
              @IntoMap
              @StringKey(`value` = "main")
              public fun bindToHandler(): Handler = MainHandler
            }
        """.trimIndent())
    }

    @Test
    fun `AutoBindsIntoMap error when object has no interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey

            @AutoBindsIntoMap
            @StringKey("key")
            object Standalone
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must implement at least one interface or extend a super-class"
        ))
    }

    @Test
    fun `AutoBinds and AutoBindsIntoSet on same object generate separate modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet

            interface Interceptor

            @AutoBinds
            @AutoBindsIntoSet
            object DefaultInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("DefaultInterceptorModule.kt").assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object DefaultInterceptorModule {
              @Provides
              public fun bindToInterceptor(): Interceptor = DefaultInterceptor
            }
        """.trimIndent())

        result.assertHasGeneratedFile("DefaultInterceptor__IntoSetModule.kt").assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal object DefaultInterceptor__IntoSetModule {
              @Provides
              @IntoSet
              public fun bindToInterceptor(): Interceptor = DefaultInterceptor
            }
        """.trimIndent())
    }

    @Test
    fun `all three annotations on same object generate all three modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey

            interface Interceptor

            @AutoBinds
            @AutoBindsIntoSet
            @AutoBindsIntoMap
            @StringKey("logging")
            object LoggingInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptorModule.kt").assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptorModule {
              @Provides
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())

        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt").assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptor__IntoSetModule {
              @Provides
              @IntoSet
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())

        result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal object LoggingInterceptor__IntoMapModule {
              @Provides
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(): Interceptor = LoggingInterceptor
            }
        """.trimIndent())
    }

}
