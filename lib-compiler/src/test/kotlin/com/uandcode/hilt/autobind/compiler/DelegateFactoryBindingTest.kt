package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
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
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object MyDbModule {
              @Provides
              public fun provideMyDb(factory: MyDbFactory): MyDb = factory.provideDelegate()

              @Provides
              public fun provideNoteDao(`delegate`: MyDb): NoteDao = delegate.noteDao()

              @Provides
              public fun provideOrderDao(`delegate`: MyDb): OrderDao = delegate.orderDao()
            }
        """.trimIndent())
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
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object MyDbModule {
              @Provides
              public fun provideMyDb(factory: MyDbFactory): MyDb = factory.provideDelegate()

              @Provides
              public fun provideNoteDao(`delegate`: MyDb): NoteDao = delegate.noteDao()
            }
        """.trimIndent())
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
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Singleton

            @Module
            @InstallIn(SingletonComponent::class)
            internal object MyDbModule {
              @Provides
              @Singleton
              public fun provideMyDb(factory: MyDbFactory): MyDb = factory.provideDelegate()

              @Provides
              public fun provideNoteDao(`delegate`: MyDb): NoteDao = delegate.noteDao()
            }
        """.trimIndent())
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
