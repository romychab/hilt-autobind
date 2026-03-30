package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AnnotationConflictTest {

    @Test
    fun `error when class has both @AutoBinds alias and direct @AutoBinds`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            @MyBind
            @AutoBinds
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "Annotation '@AutoBinds' is applied multiple times. Review all aliases and exclude duplicated annotations"
        ))
    }

    @Test
    fun `error when class has both @AutoBindsIntoSet alias and direct @AutoBindsIntoSet`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            @AutoBindsIntoSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "Annotation '@AutoBindsIntoSet' is applied multiple times. " +
                    "Review all aliases and exclude duplicated annotations"
        ))
    }

    @Test
    fun `@AutoBinds alias combined with direct @AutoBindsIntoSet is valid`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            @MyBind
            @AutoBindsIntoSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        result.assertHasGeneratedFile("LoggingInterceptorModule.kt")
        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
    }

    @Test
    fun `@AutoBindsIntoSet alias combined with direct @AutoBinds is valid`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @ContributeToSet
            @AutoBinds
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        result.assertHasGeneratedFile("LoggingInterceptorModule.kt")
        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
    }

    @Test
    fun `@AutoBinds alias combined with @AutoBindsIntoSet alias is valid`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            annotation class ContributeToSet

            @MyBind
            @ContributeToSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        result.assertHasGeneratedFile("LoggingInterceptorModule.kt")
        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
    }

    @Test
    fun `error when two aliases use same annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind1

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind2

            @MyBind1
            @MyBind2
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "Annotation '@AutoBinds' is applied multiple times. Review all aliases and exclude duplicated annotations"
        ))
    }
}
