package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntoSetBindingTest {

    @Test
    fun `generates Binds IntoSet module`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoSet
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
    fun `AutoBinds and AutoBindsIntoSet on same class generate separate modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @AutoBinds
            @AutoBindsIntoSet
            class DefaultInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val bindsModule = result.assertHasGeneratedFile("DefaultInterceptorModule.kt")
        bindsModule.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface DefaultInterceptorModule {
              @Binds
              public fun bindToInterceptor(`impl`: DefaultInterceptor): Interceptor
            }
        """.trimIndent())

        val intoSetModule = result.assertHasGeneratedFile("DefaultInterceptor__IntoSetModule.kt")
        intoSetModule.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface DefaultInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToInterceptor(`impl`: DefaultInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `error when annotated on interface`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet

            interface Parent

            @AutoBindsIntoSet
            interface NotAClass : Parent
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("@AutoBindsIntoSet"))
        assertTrue(result.messages.contains("must be a class"))
    }

    @Test
    fun `error when no interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            @AutoBindsIntoSet
            class Standalone @Inject constructor()
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must implement at least one interface"))
    }

    @Test
    fun `uses explicit installIn Singleton`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoSet(installIn = HiltComponent.Singleton)
            class ScopedInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ScopedInterceptor__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface ScopedInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToInterceptor(`impl`: ScopedInterceptor): Interceptor
            }
        """.trimIndent())
    }
}
