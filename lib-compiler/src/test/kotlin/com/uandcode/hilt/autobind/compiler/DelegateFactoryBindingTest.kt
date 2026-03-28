package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DelegateFactoryBindingTest {

    @Test
    fun `generates Provides for delegate and sub-methods`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            interface NoteDao
            interface OrderDao

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb> {
                override fun provideDelegate(): MyDb {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyDbFactory::class)
            abstract class MyDb {
                abstract fun noteDao(): NoteDao
                abstract fun orderDao(): OrderDao
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyDbModule.kt")
        assertTrue(generated.contains("fun provideMyDb"))
        assertTrue(generated.contains("factory.provideDelegate()"))
        assertTrue(generated.contains("fun provideNoteDao"))
        assertTrue(generated.contains("delegate.noteDao()"))
        assertTrue(generated.contains("fun provideOrderDao"))
        assertTrue(generated.contains("delegate.orderDao()"))
        assertTrue(generated.contains("internal object MyDbModule"))
    }

    @Test
    fun `generates Provides for declared methods`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            interface NoteDao

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb> {
                override fun provideDelegate(): MyDb {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyDbFactory::class)
            abstract class MyDb {
                abstract fun noteDao(): NoteDao
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyDbModule.kt")
        assertTrue(generated.contains("fun provideNoteDao"))
        assertTrue(generated.contains("delegate.noteDao()"))
    }

    @Test
    fun `adds scope only to main delegate when @AutoScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import com.uandcode.hilt.autobind.factories.AutoScoped
            import javax.inject.Inject

            interface NoteDao

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb> {
                @AutoScoped
                override fun provideDelegate(): MyDb {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyDbFactory::class)
            abstract class MyDb {
                abstract fun noteDao(): NoteDao
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyDbModule.kt")
        // The main provide method should have @Singleton
        assertTrue(generated.contains("@Singleton"))
        // Count occurrences: @Singleton should appear only once (for provideMyDb, not provideNoteDao)
        val singletonCount = Regex("@Singleton").findAll(generated).count()
        assertTrue(singletonCount == 1, "Expected @Singleton once but found $singletonCount times")
    }

    @Test
    fun `error when factory has type parameters`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            interface Repo

            class GenericFactory<T : Any> @Inject constructor() : DelegateBindingFactory<T> {
                override fun provideDelegate(): T {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = GenericFactory::class)
            abstract class MyRepo : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must not have type parameters"))
    }

    @Test
    fun `error when factory is an object`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            interface Repo

            object SingletonFactory : DelegateBindingFactory<Repo> {
                override fun provideDelegate(): Repo {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = SingletonFactory::class)
            abstract class MyRepo : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must be a class"))
    }

    @Test
    fun `error when factory is abstract`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            interface Repo

            abstract class AbstractFactory @Inject constructor() : DelegateBindingFactory<Repo> {
                override fun provideDelegate(): Repo {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = AbstractFactory::class)
            abstract class MyRepo : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must not be abstract"))
    }

    @Test
    fun `error when factory is open`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            interface Repo

            open class OpenFactory @Inject constructor() : DelegateBindingFactory<Repo> {
                override fun provideDelegate(): Repo {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = OpenFactory::class)
            abstract class MyRepo : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must be a final class"))
    }

    @Test
    fun `error when factory has no @Inject constructor`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory

            interface Repo

            class NoInjectFactory : DelegateBindingFactory<Repo> {
                override fun provideDelegate(): Repo {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = NoInjectFactory::class)
            abstract class MyRepo : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must have a primary constructor with @Inject annotation"))
    }

    @Test
    fun `error when annotated class has type parameters`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb<*>> {
                override fun provideDelegate(): MyDb<*> {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyDbFactory::class)
            abstract class MyDb<T>
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("should not have type parameters"))
    }

    @Test
    fun `warns when @AutoScoped is placed on sub-delegate method instead of provideDelegate`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import com.uandcode.hilt.autobind.factories.AutoScoped
            import javax.inject.Inject

            interface NoteDao

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb> {
                override fun provideDelegate(): MyDb {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyDbFactory::class)
            abstract class MyDb {
                @AutoScoped
                abstract fun noteDao(): NoteDao
            }
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        assertTrue(result.messages.contains("@AutoScoped on 'noteDao()' is ignored"))
        assertTrue(result.messages.contains("Place @AutoScoped on MyDbFactory.provideDelegate()"))
    }

    @Test
    fun `error when provideDelegate returns wrong type`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject

            class OtherClass

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<OtherClass> {
                override fun provideDelegate(): OtherClass {
                    throw UnsupportedOperationException()
                }
            }

            @AutoBinds(factory = MyDbFactory::class)
            abstract class MyDb
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("The provideDelegate() function must return 'test.MyDb'"))
    }
}
