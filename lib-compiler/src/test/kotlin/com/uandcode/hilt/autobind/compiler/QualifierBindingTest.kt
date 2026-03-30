package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QualifierBindingTest {

    @Test
    fun `named qualifier on class appears on @Binds function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Named

            interface UserRepository

            @Named("main")
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
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface UserRepositoryImplModule {
              @Binds
              @Named(`value` = "main")
              public fun bindToUserRepository(`impl`: UserRepositoryImpl): UserRepository
            }
        """.trimIndent())
    }

    @Test
    fun `custom qualifier annotation on class appears on @Binds function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class DbQualifier

            interface Repo

            @DbQualifier
            @AutoBinds
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
              @DbQualifier
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `qualifier on alias is forwarded to generated @Binds function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class DbQualifier

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @DbQualifier
            annotation class BindDb

            interface Repo

            @BindDb
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
              @DbQualifier
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `qualifier and scope on alias both appear on generated @Binds function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Named
            import javax.inject.Singleton

            interface Repo

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @Singleton
            @Named("main")
            annotation class BindMainSingleton

            @BindMainSingleton
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
            import javax.inject.Named
            import javax.inject.Singleton

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface RepoImplModule {
              @Binds
              @Singleton
              @Named(`value` = "main")
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `same qualifier type on class and alias is valid and produces one qualifier annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class DbQualifier

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @DbQualifier
            annotation class BindDb

            interface Repo

            @DbQualifier
            @BindDb
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
              @DbQualifier
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `error when class and alias carry different qualifier annotations`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class QualifierA

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class QualifierB

            interface Repo

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @QualifierA
            annotation class BindWithA

            @QualifierB
            @BindWithA
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting qualifiers"))
    }

    @Test
    fun `qualifier on @AutoBindsIntoSet class appears on @Binds @IntoSet function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject
            import javax.inject.Named

            interface Interceptor

            @Named("logging")
            @AutoBindsIntoSet
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              @Named(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `qualifier on @AutoBindsIntoSet alias is forwarded to generated @Binds @IntoSet function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import javax.inject.Inject
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class HandlerQualifier

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoSet
            @HandlerQualifier
            annotation class ContributeHandler

            @ContributeHandler
            class MainHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainHandler__IntoSetModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MainHandler__IntoSetModule {
              @Binds
              @IntoSet
              @HandlerQualifier
              public fun bindToHandler(`impl`: MainHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `qualifier on @AutoBinds with ClassBindingFactory appears on @Provides function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import javax.inject.Inject
            import javax.inject.Named
            import kotlin.reflect.KClass

            class RetrofitFactory @Inject constructor() : ClassBindingFactory {
                override fun <T : Any> create(kClass: KClass<T>): T {
                    throw UnsupportedOperationException()
                }
            }

            @Named("retrofit")
            @AutoBinds(factory = RetrofitFactory::class)
            interface BooksApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("BooksApiModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal object BooksApiModule {
              @Provides
              @Named(`value` = "retrofit")
              public fun provideBooksApi(factory: RetrofitFactory): BooksApi = factory.create(BooksApi::class)
            }
        """.trimIndent())
    }

    @Test
    fun `qualifier on @AutoBinds with DelegateFactory appears only on primary @Provides`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject
            import javax.inject.Named

            interface NoteDao

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb> {
                override fun provideDelegate(): MyDb {
                    throw UnsupportedOperationException()
                }
            }

            @Named("main")
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
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal object MyDbModule {
              @Provides
              @Named(`value` = "main")
              public fun provideMyDb(factory: MyDbFactory): MyDb = factory.provideDelegate()

              @Provides
              public fun provideNoteDao(@Named(`value` = "main") `delegate`: MyDb): NoteDao = delegate.noteDao()
            }
        """.trimIndent())
    }
}
