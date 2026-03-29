package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HiltComponentResolverTest {

    @Test
    fun `defaults to SingletonComponent when no scope annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            interface Repo

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
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `auto-detects SingletonComponent from @Singleton`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject
            import javax.inject.Singleton

            interface Repo

            @Singleton
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
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `auto-detects ActivityRetainedComponent from @ActivityRetainedScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityRetainedScoped
            import javax.inject.Inject

            interface Repo

            @ActivityRetainedScoped
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
            import dagger.hilt.android.components.ActivityRetainedComponent

            @Module
            @InstallIn(ActivityRetainedComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `auto-detects ActivityComponent from @ActivityScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            interface Repo

            @ActivityScoped
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
    fun `auto-detects ViewModelComponent from @ViewModelScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ViewModelScoped
            import javax.inject.Inject

            interface Repo

            @ViewModelScoped
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
            import dagger.hilt.android.components.ViewModelComponent

            @Module
            @InstallIn(ViewModelComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `auto-detects FragmentComponent from @FragmentScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.FragmentScoped
            import javax.inject.Inject

            interface Repo

            @FragmentScoped
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
            import dagger.hilt.android.components.FragmentComponent

            @Module
            @InstallIn(FragmentComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `auto-detects ViewComponent from @ViewScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ViewScoped
            import javax.inject.Inject

            interface Repo

            @ViewScoped
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
            import dagger.hilt.android.components.ViewComponent

            @Module
            @InstallIn(ViewComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `auto-detects ServiceComponent from @ServiceScoped`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import dagger.hilt.android.scopes.ServiceScoped
            import javax.inject.Inject

            interface Repo

            @ServiceScoped
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
            import dagger.hilt.android.components.ServiceComponent

            @Module
            @InstallIn(ServiceComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `explicit installIn Singleton`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.Singleton)
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
    fun `explicit installIn ActivityRetained`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.ActivityRetained)
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
            import dagger.hilt.android.components.ActivityRetainedComponent

            @Module
            @InstallIn(ActivityRetainedComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `explicit installIn Activity`() {
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
        result.assertOk()

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
    fun `explicit installIn ViewModel`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.ViewModel)
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
            import dagger.hilt.android.components.ViewModelComponent

            @Module
            @InstallIn(ViewModelComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `explicit installIn Fragment`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.Fragment)
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
            import dagger.hilt.android.components.FragmentComponent

            @Module
            @InstallIn(FragmentComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `explicit installIn View`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.View)
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
            import dagger.hilt.android.components.ViewComponent

            @Module
            @InstallIn(ViewComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `explicit installIn ViewWithFragment`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.ViewWithFragment)
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
            import dagger.hilt.android.components.ViewWithFragmentComponent

            @Module
            @InstallIn(ViewWithFragmentComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `explicit installIn Service`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject

            interface Repo

            @AutoBinds(installIn = HiltComponent.Service)
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
            import dagger.hilt.android.components.ServiceComponent

            @Module
            @InstallIn(ServiceComponent::class)
            internal interface RepoImplModule {
              @Binds
              public fun bindToRepo(`impl`: RepoImpl): Repo
            }
        """.trimIndent())
    }

    @Test
    fun `valid when scope annotation matches explicit installIn`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject
            import javax.inject.Singleton

            interface Repo

            @Singleton
            @AutoBinds(installIn = HiltComponent.Singleton)
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
    fun `error when scope annotation mismatches explicit installIn`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import javax.inject.Inject
            import javax.inject.Singleton

            interface Repo

            @Singleton
            @AutoBinds(installIn = HiltComponent.Activity)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("@Singleton"))
        assertTrue(result.messages.contains("Activity"))
    }

    @Test
    fun `error when ActivityScoped mismatches explicit installIn Singleton`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent
            import dagger.hilt.android.scopes.ActivityScoped
            import javax.inject.Inject

            interface Repo

            @ActivityScoped
            @AutoBinds(installIn = HiltComponent.Singleton)
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("ActivityScoped"))
        assertTrue(result.messages.contains("Singleton"))
    }
}
