package com.uandcode.hilt.autobind.compiler.intomap

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IntoMapErrorTest {

    @Test
    fun `error when two different MapKey annotations on class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.IntKey
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoMap
            @StringKey("a")
            @IntKey(1)
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("only one @MapKey annotation is allowed, " +
                "but found: [@StringKey, @IntKey]"))
    }

    @Test
    fun `error when class and alias have conflicting map keys`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.IntKey
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @StringKey("plugin")
            annotation class ContributeToMap

            @ContributeToMap
            @IntKey(1)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys. Class has @IntKey but alias has @StringKey"))
    }

    @Test
    fun `error when annotated class is abstract`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoMap
            abstract class AbstractInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("The class 'AbstractInterceptor' annotated " +
                "with @AutoBindsIntoMap annotation must be a non-abstract class"))
    }

    @Test
    fun `error when class has no Inject constructor`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap

            interface Interceptor

            @AutoBindsIntoMap
            class LoggingInterceptor : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("The class 'LoggingInterceptor' annotated with @AutoBindsIntoMap " +
                "annotation must have a primary constructor with @Inject annotation"))
    }

    @Test
    fun `error when class has no supertypes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import javax.inject.Inject

            @AutoBindsIntoMap
            class Standalone @Inject constructor()
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("The class 'Standalone' annotated with @AutoBindsIntoMap " +
                "annotation must implement at least one interface or extend a super-class"))
    }

}
