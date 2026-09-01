package com.uandcode.hilt.autobind.compiler.optional

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OptionalOfErrorTest {

    @Test
    fun `error when applied to concrete class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            interface DebugPanel

            @AutoBindsOptionalOf
            class DebugPanelImpl : DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must be applied to an interface or abstract class. Did you mean @AutoBinds?"
        ), result.messages)
    }

    @Test
    fun `error when applied to object`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            interface DebugPanel

            @AutoBindsOptionalOf
            object NoOpDebugPanel : DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must be applied to an interface or abstract class. Did you mean @AutoBinds?"
        ), result.messages)
    }

    @Test
    fun `error when applied to enum class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @AutoBindsOptionalOf
            enum class Mode { DEBUG, RELEASE }
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must be applied to an interface or abstract class. Did you mean @AutoBinds?"
        ), result.messages)
    }

    @Test
    fun `error when interface has type parameters`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @AutoBindsOptionalOf
            interface Repo<T>
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must not have type parameters"), result.messages)
    }

    @Test
    fun `error when inner class is annotated`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            class Outer {
                @AutoBindsOptionalOf
                abstract inner class InnerTools
            }
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must not be an inner class (remove the 'inner' keyword)"
        ), result.messages)
    }

    @Test
    fun `error when class has both an alias and a direct AutoBindsOptionalOf`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @Target(AnnotationTarget.CLASS)
            @AutoBindsOptionalOf
            annotation class OptionalDebug

            @OptionalDebug
            @AutoBindsOptionalOf
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "Annotation '@AutoBindsOptionalOf' is applied multiple times. " +
                    "Review all aliases and exclude duplicated annotations"
        ), result.messages)
    }
}
