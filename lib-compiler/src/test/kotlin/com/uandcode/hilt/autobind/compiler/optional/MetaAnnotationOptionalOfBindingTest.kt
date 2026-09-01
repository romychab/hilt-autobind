package com.uandcode.hilt.autobind.compiler.optional

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MetaAnnotationOptionalOfBindingTest {

    @Test
    fun `alias with baked-in installIn generates module in that component`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import com.uandcode.hilt.autobind.HiltComponent

            @AutoBindsOptionalOf(installIn = HiltComponent.Activity)
            @Target(AnnotationTarget.CLASS)
            annotation class ActivityOptional

            @ActivityOptional
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
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface DebugPanel__OptionalModule {
              @BindsOptionalOf
              public fun optionalDebugPanel(): DebugPanel
            }
        """.trimIndent())
    }

    @Test
    fun `alias carrying a scope selects the component without emitting the scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import dagger.hilt.android.scopes.ActivityScoped

            @AutoBindsOptionalOf
            @ActivityScoped
            @Target(AnnotationTarget.CLASS)
            annotation class ActivityOptional

            @ActivityOptional
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DebugPanel__OptionalModule.kt")
        assertTrue(generated.contains("@InstallIn(ActivityComponent::class)"), generated)
        assertFalse(generated.contains("ActivityScoped"), generated)
    }

    @Test
    fun `alias carrying a qualifier forwards it to the function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import javax.inject.Named

            @AutoBindsOptionalOf
            @Named("debug")
            @Target(AnnotationTarget.CLASS)
            annotation class DebugOptional

            @DebugOptional
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DebugPanel__OptionalModule.kt")
        assertTrue(generated.contains("""@Named(`value` = "debug")"""), generated)
    }

    @Test
    fun `generates metadata carrier for an AutoBindsOptionalOf alias`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @Target(AnnotationTarget.CLASS)
            @AutoBindsOptionalOf
            annotation class OptionalDebug
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("__test__OptionalDebug.kt")
        generated.assertContent("""
            package com.uandcode.hilt.autobind.metadata

            import com.uandcode.hilt.autobind.MetaAutoBindingInfo

            @MetaAutoBindingInfo(qualifiedMetaAnnotationName = "test.OptionalDebug")
            internal class __test__OptionalDebug
        """.trimIndent())
    }

    @Test
    fun `alias from another module is processed`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.metadata.MultiModuleOptionalActivity

            @MultiModuleOptionalActivity
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
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface DebugPanel__OptionalModule {
              @BindsOptionalOf
              public fun optionalDebugPanel(): DebugPanel
            }
        """.trimIndent())
    }

    @Test
    fun `error when alias does not target classes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @Target(AnnotationTarget.FUNCTION)
            @AutoBindsOptionalOf
            annotation class OptionalDebug
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "must declare @Target(AnnotationTarget.CLASS) to be applied to classes."
        ), result.messages)
    }
}
