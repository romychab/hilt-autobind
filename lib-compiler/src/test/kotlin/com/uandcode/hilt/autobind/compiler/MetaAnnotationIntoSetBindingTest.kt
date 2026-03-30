package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertNoGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MetaAnnotationIntoSetBindingTest {

    @Test
    fun `basic alias generates __IntoSetModule with @Binds @IntoSet`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `alias forwards installIn parameter`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet(installIn = HiltComponent.Activity)
            annotation class ContributeActivityHandler

            @ContributeActivityHandler
            class MainHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainHandler__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainHandler__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToHandler(`impl`: MainHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `alias forwards bindTo parameter`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor
            interface Closeable

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet(bindTo = [Interceptor::class])
            annotation class ContributeInterceptor

            @ContributeInterceptor
            class LoggingInterceptor @Inject constructor() : Interceptor, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `scope annotation on alias auto-detects component`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            @ActivityScoped
            annotation class ContributeActivityScopedHandler

            @ContributeActivityScopedHandler
            class MainHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainHandler__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainHandler__IntoSetModule {
              @Binds
              @IntoSet
              @ActivityScoped
              public fun bindToHandler(`impl`: MainHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `multiple classes using the same alias generate separate __IntoSetModule files`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            class LoggingInterceptor @Inject constructor() : Interceptor

            @ContributeToSet
            class AuthInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        result.assertHasGeneratedFile("AuthInterceptor__IntoSetModule.kt")
    }

    @Test
    fun `scope on annotated class is respected when alias has no scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeHandler

            @ActivityScoped
            @ContributeHandler
            class MainHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainHandler__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainHandler__IntoSetModule {
              @Binds
              @IntoSet
              @ActivityScoped
              public fun bindToHandler(`impl`: MainHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `error when meta-annotation lacks @Target(AnnotationTarget CLASS)`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet

            @Target(AnnotationTarget.FUNCTION)
            @AutoBindsIntoSet
            annotation class ContributeToSet
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must declare @Target(AnnotationTarget.CLASS)"))
    }

    @Test
    fun `error when meta-annotation has no @Target`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet

            @AutoBindsIntoSet
            annotation class ContributeToSet
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must declare @Target(AnnotationTarget.CLASS)"))
    }

    @Test
    fun `error when annotated class is abstract`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            abstract class AbstractInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
    }

    @Test
    fun `error when bindTo on alias targets a non-supertype of the annotated class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor
            interface Unrelated

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet(bindTo = [Unrelated::class])
            annotation class ContributeToSet

            @ContributeToSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("is not a supertype of 'LoggingInterceptor'"))
    }

    @Test
    fun `generated file name suffix is __IntoSetModule`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        result.assertNoGeneratedFile("LoggingInterceptorModule.kt")
    }

    @Test
    fun `alias and direct @AutoBindsIntoSet coexist on separate classes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            class LoggingInterceptor @Inject constructor() : Interceptor

            @AutoBindsIntoSet
            class AuthInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        result.assertHasGeneratedFile("AuthInterceptor__IntoSetModule.kt")
    }
}
