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

class OptionalOfComponentTest {

    @Test
    fun `explicit installIn selects the component`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import com.uandcode.hilt.autobind.HiltComponent

            @AutoBindsOptionalOf(installIn = HiltComponent.Activity)
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
    fun `scope annotation selects the component but is not emitted`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import dagger.hilt.android.scopes.ActivityScoped

            @AutoBindsOptionalOf
            @ActivityScoped
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DebugPanel__OptionalModule.kt")
        assertTrue(generated.contains("@InstallIn(ActivityComponent::class)"), generated)
        assertFalse(generated.contains("ActivityScoped"), generated)
    }

    @Test
    fun `installInCustomComponent selects the custom component without emitting its scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Scope

            @Scope
            @Retention(AnnotationRetention.RUNTIME)
            annotation class MyCustomScoped

            @MyCustomScoped
            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @AutoBindsOptionalOf(installInCustomComponent = MyCustomComponent::class)
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

            @Module
            @InstallIn(MyCustomComponent::class)
            internal interface DebugPanel__OptionalModule {
              @BindsOptionalOf
              public fun optionalDebugPanel(): DebugPanel
            }
        """.trimIndent())
    }

    @Test
    fun `error when installIn and installInCustomComponent are both set`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.DefineComponent
            import dagger.hilt.components.SingletonComponent

            @DefineComponent(parent = SingletonComponent::class)
            interface MyCustomComponent

            @AutoBindsOptionalOf(
                installIn = HiltComponent.Activity,
                installInCustomComponent = MyCustomComponent::class,
            )
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "conflicting component specifications: use either 'installIn' or 'installInCustomComponent', not both."
        ), result.messages)
    }

    @Test
    fun `error when installIn conflicts with the scope annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.android.scopes.ActivityScoped

            @AutoBindsOptionalOf(installIn = HiltComponent.Fragment)
            @ActivityScoped
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("has conflicting scopes"), result.messages)
    }

    @Test
    fun `error when alias scope conflicts with scope on the annotated type`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.hilt.android.scopes.FragmentScoped

            @AutoBindsOptionalOf
            @ActivityScoped
            @Target(AnnotationTarget.CLASS)
            annotation class ActivityOptional

            @ActivityOptional
            @FragmentScoped
            interface DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("has conflicting scopes"), result.messages)
    }
}
