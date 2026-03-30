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

class DefaultBindingTest {

    @Test
    fun `generates Binds module for simple interface`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface UserRepository

            @AutoBinds
            class UserRepositoryImpl @Inject constructor() : UserRepository
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("UserRepositoryImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface UserRepositoryImplModule {
              @Binds
              public fun bindToUserRepository(`impl`: UserRepositoryImpl): UserRepository
            }
        """.trimIndent())
    }

    @Test
    fun `generates bindings for multiple interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Logger
            interface Closeable

            @AutoBinds
            class LoggerImpl @Inject constructor() : Logger, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggerImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggerImplModule {
              @Binds
              public fun bindToLogger(`impl`: LoggerImpl): Logger

              @Binds
              public fun bindToCloseable(`impl`: LoggerImpl): Closeable
            }
        """.trimIndent())
    }

    @Test
    fun `error when class has no interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @AutoBinds
            class Standalone @Inject constructor()
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must implement at least one interface " +
                "or extend a super-class"))
    }

    @Test
    fun `error when annotated on interface without interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            interface Parent

            @AutoBinds
            interface NotAClass : Parent
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must be a class"))
    }

    @Test
    fun `error when class is abstract`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo

            @AutoBinds
            abstract class AbstractRepo @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must be a non-abstract class"))
    }

    @Test
    fun `error when no @Inject constructor`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            interface Repo

            @AutoBinds
            class RepoImpl : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must have a primary constructor with @Inject annotation"))
    }

    @Test
    fun `uses explicit installIn Activity component`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.Activity)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)

        val generated = result.assertHasGeneratedFile("RepoImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `generates Binds module for interface with generic parameter`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repository<T>

            @AutoBinds
            class StringRepository @Inject constructor() : Repository<String>
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("StringRepositoryModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import kotlin.String

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface StringRepositoryModule {
              @Binds
              public fun bindToRepository(`impl`: StringRepository): Repository<String>
            }
        """.trimIndent())
    }

    @Test
    fun `error when class is an inner class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo

            class Outer {
                @AutoBinds
                inner class RepoImpl @Inject constructor() : Repo
            }
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must not be an inner class"))
    }

    @Test
    fun `handles duplicate interface simple names with package disambiguation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Closeable

            @AutoBinds
            class MyImpl @Inject constructor() : Closeable, java.io.Closeable {
                override fun close() {}
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyImplModule {
              @Binds
              public fun bindToCloseable_test(`impl`: MyImpl): Closeable

              @Binds
              public fun bindToCloseable_java_io(`impl`: MyImpl): java.io.Closeable
            }
        """.trimIndent())
    }

    @Test
    fun `binds direct parent classes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            open class ParentClass

            @AutoBinds
            class ChildClass @Inject constructor() : ParentClass()
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ChildClassModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface ChildClassModule {
              @Binds
              public fun bindToParentClass(`impl`: ChildClass): ParentClass
            }
        """.trimIndent())
    }

    @Test
    fun `binds mixed direct parent class and interfaces`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            open class ParentClass
            interface Interface

            @AutoBinds
            class ChildClass @Inject constructor() : ParentClass(), Interface
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ChildClassModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface ChildClassModule {
              @Binds
              public fun bindToParentClass(`impl`: ChildClass): ParentClass
              @Binds
              public fun bindToInterface(`impl`: ChildClass): Interface
            }
        """.trimIndent())
    }

    @Test
    fun `no bindings for grand-parents`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            open class GrandParentClass
            open class ParentClass : GrandParentClass()

            @AutoBinds
            class ChildClass @Inject constructor() : ParentClass()
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ChildClassModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface ChildClassModule {
              @Binds
              public fun bindToParentClass(`impl`: ChildClass): ParentClass
            }
        """.trimIndent())
    }
}
