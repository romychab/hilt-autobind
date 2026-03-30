package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MetaAnnotationBindingTest {

    @Test
    fun `generates Binds module when class uses a meta-annotation alias for AutoBinds`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            interface UserRepository

            @MyBind
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
    fun `meta-annotation forwards factory parameter`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import javax.inject.Inject
            import kotlin.reflect.KClass

            interface BooksApi

            class RetrofitFactory @Inject constructor() : ClassBindingFactory {
                override fun <T : Any> create(kClass: KClass<T>): T { 
                    throw UnsupportedOperationException() 
                }
            }

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(factory = RetrofitFactory::class)
            annotation class BindRetrofit

            @BindRetrofit
            interface BooksApiImpl : BooksApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("BooksApiImplModule.kt")
        generated.assertContent("""
            package test

            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal object BooksApiImplModule {
              @Provides
              public fun provideBooksApiImpl(factory: RetrofitFactory): BooksApiImpl = factory.create(BooksApiImpl::class)
            }
        """.trimIndent())
    }

    @Test
    fun `meta-annotation forwards installIn parameter`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Presenter

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installIn = HiltComponent.Activity)
            annotation class BindToActivity

            @BindToActivity
            class MainPresenter @Inject constructor() : Presenter
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainPresenterModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainPresenterModule {
              @Binds
              public fun bindToPresenter(`impl`: MainPresenter): Presenter
            }
        """.trimIndent())
    }

    @Test
    fun `multiple classes can use the same meta-annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            interface RepoA
            interface RepoB

            @MyBind
            class RepoAImpl @Inject constructor() : RepoA

            @MyBind
            class RepoBImpl @Inject constructor() : RepoB
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result
            .assertHasGeneratedFile("RepoAImplModule.kt")
            .assertContent("""
                package test

                import dagger.Binds
                import dagger.Module
                import dagger.hilt.InstallIn
                import dagger.hilt.components.SingletonComponent

                @Module
                @InstallIn(SingletonComponent::class)
                internal interface RepoAImplModule {
                  @Binds
                  public fun bindToRepoA(`impl`: RepoAImpl): RepoA
                }
            """.trimIndent())

        result
            .assertHasGeneratedFile("RepoBImplModule.kt")
            .assertContent("""
                package test

                import dagger.Binds
                import dagger.Module
                import dagger.hilt.InstallIn
                import dagger.hilt.components.SingletonComponent

                @Module
                @InstallIn(SingletonComponent::class)
                internal interface RepoBImplModule {
                  @Binds
                  public fun bindToRepoB(`impl`: RepoBImpl): RepoB
                }
            """.trimIndent())
    }

    @Test
    fun `scope annotation on the annotated class is still respected`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            interface Presenter

            @MyBind
            @ActivityScoped
            class MainPresenter @Inject constructor() : Presenter
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainPresenterModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainPresenterModule {
              @Binds
              public fun bindToPresenter(`impl`: MainPresenter): Presenter
            }
        """.trimIndent())
    }

    @Test
    fun `error when meta-annotation lacks @Target(AnnotationTarget CLASS)`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @Target(AnnotationTarget.FUNCTION)
            @AutoBinds
            annotation class MyBind

            interface Repo

            @MyBind
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must declare @Target(AnnotationTarget.CLASS)"))
    }

    @Test
    fun `meta-annotation with no @Target is not acceptable`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @AutoBinds
            annotation class MyBind

            interface Repo

            @MyBind
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must declare @Target(AnnotationTarget.CLASS)"))
    }

    @Test
    fun `error when scope annotation on meta-annotation does not match installIn component`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            interface Presenter

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installIn = HiltComponent.Fragment)
            @ActivityScoped
            annotation class BindToActivity

            @BindToActivity
            class MainPresenter @Inject constructor() : Presenter
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()

        assertTrue(result.messages.contains(
            "class has conflicting scopes: " +
                    "[dagger.hilt.android.scopes.FragmentScoped, dagger.hilt.android.scopes.ActivityScoped]. " +
                    "Make sure you align installIn=... param with Scope annotation"
        ))
    }

    @Test
    fun `scope annotation on meta-annotation auto-detects component for Binds module`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            interface Presenter

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @ActivityScoped
            annotation class BindToActivity

            @BindToActivity
            class MainPresenter @Inject constructor() : Presenter
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainPresenterModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.hilt.android.scopes.ActivityScoped
            
            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainPresenterModule {
              @Binds
              @ActivityScoped
              public fun bindToPresenter(`impl`: MainPresenter): Presenter
            }
        """.trimIndent())
    }

    @Test
    fun `scope annotation on meta-annotation generates scoped Provides with ClassFactory`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.ClassBindingFactory
            import javax.inject.Inject
            import javax.inject.Singleton
            import kotlin.reflect.KClass

            class RetrofitFactory @Inject constructor() : ClassBindingFactory {
                override fun <T : Any> create(kClass: KClass<T>): T {
                    throw UnsupportedOperationException()
                }
            }

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(factory = RetrofitFactory::class)
            @Singleton
            annotation class BindRetrofit

            @BindRetrofit
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
            import javax.inject.Singleton

            @Module
            @InstallIn(SingletonComponent::class)
            internal object BooksApiModule {
              @Provides
              @Singleton
              public fun provideBooksApi(factory: RetrofitFactory): BooksApi = factory.create(BooksApi::class)
            }
        """.trimIndent())
    }

    @Test
    fun `scope annotation on meta-annotation generates scoped Provides with DelegateFactory`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
            import javax.inject.Inject
            import javax.inject.Singleton

            interface NoteDao

            class MyDbFactory @Inject constructor() : DelegateBindingFactory<MyDb> {
                override fun provideDelegate(): MyDb {
                    throw UnsupportedOperationException()
                }
            }

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(factory = MyDbFactory::class)
            @Singleton
            annotation class BindDb

            @BindDb
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
    fun `error when class scope conflicts with meta-annotation scope`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject
            import javax.inject.Singleton

            interface Presenter

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            @Singleton
            annotation class MySingletonBind

            @MySingletonBind
            @ActivityScoped
            class MainPresenter @Inject constructor() : Presenter
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains(
            "class has conflicting scopes: " +
                    "[dagger.hilt.android.scopes.ActivityScoped, javax.inject.Singleton]. " +
                    "Make sure you align installIn=... param with Scope annotation"
        ))
    }

    @Test
    fun `bindTo on meta-annotation restricts bindings to specified subset of supertypes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo
            interface Closeable

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(bindTo = [Repo::class])
            annotation class BindToRepo

            @BindToRepo
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
    fun `bindTo on meta-annotation can target a grandparent class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface GrandParent
            open class Parent : GrandParent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(bindTo = [GrandParent::class])
            annotation class BindToGrandParent

            @BindToGrandParent
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
    fun `error when bindTo on meta-annotation targets a non-supertype of the annotated class`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo
            interface Unrelated

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(bindTo = [Unrelated::class])
            annotation class BindToUnrelated

            @BindToUnrelated
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("is not a supertype of 'RepoImpl'"))
    }

    @Test
    fun `aliases on aliases are not supported`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class Alias1

            @Target(AnnotationTarget.CLASS)
            @Alias1
            annotation class Alias2

            @Alias2
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("must be a class"))
    }

}
