package com.uandcode.hilt.autobind.compiler.optional

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Test

class OptionalOfBindingTest {

    @Test
    fun `generates BindsOptionalOf module for interface`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @AutoBindsOptionalOf
            interface DebugPanel {
                fun show()
            }
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

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface DebugPanel__OptionalModule {
              @BindsOptionalOf
              public fun optionalDebugPanel(): DebugPanel
            }
        """.trimIndent())
    }

    @Test
    fun `generates BindsOptionalOf module for abstract class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            @AutoBindsOptionalOf
            abstract class DebugTools {
                abstract fun open()
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DebugTools__OptionalModule.kt")
        generated.assertContent("""
            package test

            import dagger.BindsOptionalOf
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface DebugTools__OptionalModule {
              @BindsOptionalOf
              public fun optionalDebugTools(): DebugTools
            }
        """.trimIndent())
    }

    @Test
    fun `generates module for nested interface with double underscore name`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsOptionalOf

            class Outer {
                @AutoBindsOptionalOf
                interface Inner
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("Outer__Inner__OptionalModule.kt")
        generated.assertContent("""
            package test

            import dagger.BindsOptionalOf
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface Outer__Inner__OptionalModule {
              @BindsOptionalOf
              public fun optionalInner(): Outer.Inner
            }
        """.trimIndent())
    }

    @Test
    fun `optional declaration coexists with an AutoBinds implementation of the same type`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import javax.inject.Inject

            @AutoBindsOptionalOf
            interface DebugPanel

            @AutoBinds
            class DebugPanelImpl @Inject constructor() : DebugPanel
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("DebugPanel__OptionalModule.kt")
        result.assertHasGeneratedFile("DebugPanelImplModule.kt")
    }

    @Test
    fun `AutoBinds factory and AutoBindsOptionalOf on the same interface generate separate modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsOptionalOf
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import javax.inject.Inject
            import kotlin.reflect.KClass

            class MyFactory @Inject constructor() : ClassBindingFactory {
                override fun <T : Any> create(kClass: KClass<T>): T {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyFactory::class)
            @AutoBindsOptionalOf
            interface MyApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("MyApiModule.kt")

        val optionalModule = result.assertHasGeneratedFile("MyApi__OptionalModule.kt")
        optionalModule.assertContent("""
            package test

            import dagger.BindsOptionalOf
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyApi__OptionalModule {
              @BindsOptionalOf
              public fun optionalMyApi(): MyApi
            }
        """.trimIndent())
    }
}
