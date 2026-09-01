package com.uandcode.hilt.autobind.compiler.optional

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OptionalOfQualifierTest {

    @Test
    fun `forwards Named qualifier to the BindsOptionalOf function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import javax.inject.Named

            @AutoBindsOptionalOf
            @Named("debug")
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DebugPanel__OptionalModule.kt")
        generated.assertContent("""
            package test

            import dagger.BindsOptionalOf
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface DebugPanel__OptionalModule {
              @BindsOptionalOf
              @Named(`value` = "debug")
              public fun optionalDebugPanel(): DebugPanel
            }
        """.trimIndent())
    }

    @Test
    fun `forwards custom qualifier to the BindsOptionalOf function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class DebugQualifier

            @AutoBindsOptionalOf
            @DebugQualifier
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DebugPanel__OptionalModule.kt")
        assertTrue(generated.contains("@DebugQualifier"), generated)
        assertTrue(generated.contains("public fun optionalDebugPanel(): DebugPanel"), generated)
    }

    @Test
    fun `error when class and alias declare conflicting qualifiers`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class QualifierA

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class QualifierB

            @AutoBindsOptionalOf
            @QualifierA
            @Target(AnnotationTarget.CLASS)
            annotation class OptionalDebug

            @OptionalDebug
            @QualifierB
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("has conflicting qualifiers"), result.messages)
    }
}
