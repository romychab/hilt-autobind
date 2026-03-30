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

class BindToParameterTest {

    @Test
    fun `bindTo empty list falls back to auto-detection`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo

            @AutoBinds(bindTo = [])
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `bindTo restricts bindings to specified subset of direct supertypes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo
            interface Closeable

            @AutoBinds(bindTo = [Repo::class])
            class RepoImpl @Inject constructor() : Repo, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `bindTo can target a grandparent class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface GrandParent
            open class Parent : GrandParent

            @AutoBinds(bindTo = [GrandParent::class])
            class Child @Inject constructor() : Parent()
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ChildModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface ChildModule {
              @Binds
              public fun bindToGrandParent(`impl`: Child): GrandParent
            }
        """.trimIndent())
    }

    @Test
    fun `bindTo with multiple targets generates bindings for each`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo
            interface Closeable
            interface Auditable

            @AutoBinds(bindTo = [Repo::class, Closeable::class])
            class RepoImpl @Inject constructor() : Repo, Closeable, Auditable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo

              @Binds
              public fun bindToCloseable(`impl`: RepoImpl): Closeable
            }
        """.trimIndent())
    }

    @Test
    fun `error when bindTo targets a non-supertype`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo
            interface Unrelated

            @AutoBinds(bindTo = [Unrelated::class])
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("is not a supertype of 'RepoImpl'"))
    }

    @Test
    fun `error when bindTo targets a non-supertype does not generate module`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo
            interface Unrelated

            @AutoBinds(bindTo = [Unrelated::class])
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        result.assertNoGeneratedFile("RepoImplModule.kt")
    }

    @Test
    fun `bindTo on AutoBindsIntoSet targets grandparent`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface GrandParent
            open class Parent : GrandParent

            @AutoBindsIntoSet(bindTo = [GrandParent::class])
            class Child @Inject constructor() : Parent()
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("Child__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface Child__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToGrandParent(`impl`: Child): GrandParent
            }
        """.trimIndent())
    }

    @Test
    fun `error when AutoBindsIntoSet bindTo targets a non-supertype`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject

            interface Interceptor
            interface Unrelated

            @AutoBindsIntoSet(bindTo = [Unrelated::class])
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("is not a supertype of 'LoggingInterceptor'"))
    }
}
