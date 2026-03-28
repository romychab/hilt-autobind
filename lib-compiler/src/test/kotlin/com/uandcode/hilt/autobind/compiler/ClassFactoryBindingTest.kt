package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClassFactoryBindingTest {

    @Test
    fun `generates Provides module with ClassBindingFactory`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import javax.inject.Inject
            import kotlin.reflect.KClass

            class MyFactory @Inject constructor() : ClassBindingFactory {
                override fun <T : Any> create(kClass: KClass<T>): T {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyFactory::class)
            interface MyApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyApiModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object MyApiModule {
              @Provides
              public fun provideMyApi(factory: MyFactory): MyApi = factory.create(MyApi::class)
            }
        """.trimIndent())
    }

    @Test
    fun `error when factory has no @Inject constructor`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import kotlin.reflect.KClass

            class MyFactory : ClassBindingFactory {
                override fun <T : Any> create(kClass: KClass<T>): T {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyFactory::class)
            interface MyApi
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must have a primary constructor with @Inject annotation"))
    }

    @Test
    fun `adds scope annotation when @AutoScoped on create`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import com.uandcode.hilt.autobind.factories.AutoScoped
            import javax.inject.Inject
            import kotlin.reflect.KClass

            class MyFactory @Inject constructor() : ClassBindingFactory {
                @AutoScoped
                override fun <T : Any> create(kClass: KClass<T>): T {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyFactory::class)
            interface MyApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyApiModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Singleton

            @Module
            @InstallIn(SingletonComponent::class)
            internal object MyApiModule {
              @Provides
              @Singleton
              public fun provideMyApi(factory: MyFactory): MyApi = factory.create(MyApi::class)
            }
        """.trimIndent())
    }

}
