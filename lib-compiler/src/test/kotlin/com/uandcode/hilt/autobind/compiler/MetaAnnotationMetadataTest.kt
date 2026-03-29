package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertNoGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Test

class MetaAnnotationMetadataTest {

    @Test
    fun `generates carrier class for meta-annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class BindDefault
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("__test__BindDefault.kt")
        generated.assertContent("""
            package com.uandcode.hilt.autobind.metadata

            import com.uandcode.hilt.autobind.MetaAutoBindingInfo

            @MetaAutoBindingInfo(qualifiedMetaAnnotationName = "test.BindDefault")
            internal class __test__BindDefault
        """.trimIndent())
    }

    @Test
    fun `carrier class name replaces all dots with double underscores`() {
        val source = SourceFile.kotlin("Test.kt", """
            package com.example.sub

            import com.uandcode.hilt.autobind.AutoBinds

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class BindApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("__com__example__sub__BindApi.kt")
        generated.assertContent("""
            package com.uandcode.hilt.autobind.metadata

            import com.uandcode.hilt.autobind.MetaAutoBindingInfo

            @MetaAutoBindingInfo(qualifiedMetaAnnotationName = "com.example.sub.BindApi")
            internal class __com__example__sub__BindApi
        """.trimIndent())
    }

    @Test
    fun `generates carrier class and Hilt module at the same time`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import javax.inject.Inject

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class MyBind

            interface Repo

            @MyBind
            class RepoImpl @Inject constructor() : Repo
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("__test__MyBind.kt")
        result.assertHasGeneratedFile("RepoImplModule.kt")
    }

    @Test
    fun `generates one carrier class per meta-annotation`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class BindA

            @Target(AnnotationTarget.CLASS)
            @AutoBinds
            annotation class BindB
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("__test__BindA.kt")
        result.assertHasGeneratedFile("__test__BindB.kt")
    }

    @Test
    fun `does not generate carrier class for direct @AutoBinds usage`() {
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

        result.assertHasGeneratedFile("RepoImplModule.kt")
        // Carrier classes are only generated for annotation class declarations
        result.assertNoGeneratedFile("__test__RepoImpl.kt")
    }

    @Test
    fun `carrier class qualifiedMetaAnnotationName matches the annotation fully qualified name`() {
        val source = SourceFile.kotlin("Test.kt", """
            package com.myapp.bindings

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.HiltComponent

            @Target(AnnotationTarget.CLASS)
            @AutoBinds(installIn = HiltComponent.Activity)
            annotation class ActivityBind
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("__com__myapp__bindings__ActivityBind.kt")
        generated.assertContent("""
            package com.uandcode.hilt.autobind.metadata

            import com.uandcode.hilt.autobind.MetaAutoBindingInfo

            @MetaAutoBindingInfo(qualifiedMetaAnnotationName = "com.myapp.bindings.ActivityBind")
            internal class __com__myapp__bindings__ActivityBind
        """.trimIndent())
    }

    @Test
    fun `annotation alias from other module must be processed`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.metadata.MultiModuleBindToActivity
            import javax.inject.Inject

            interface UserRepository

            @MultiModuleBindToActivity
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
            import dagger.hilt.android.components.ActivityComponent

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface UserRepositoryImplModule {
              @Binds
              public fun bindToUserRepository(`impl`: UserRepositoryImpl): UserRepository
            }
        """.trimIndent())
    }

    @Test
    fun `annotation alias with scoped class factory from other module must be processed`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.metadata.MultiModuleFactoryBinding

            @MultiModuleFactoryBinding
            interface UserApi
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        val generated = result.assertHasGeneratedFile("UserApiModule.kt")
        generated.assertContent("""
            package test

            import com.uandcode.hilt.autobind.metadata.MultiModuleFactory
            import dagger.Module
            import dagger.Provides
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import javax.inject.Singleton

            @Module
            @InstallIn(SingletonComponent::class)
            internal object UserApiModule {
              @Provides
              @Singleton
              public fun provideUserApi(factory: MultiModuleFactory): UserApi = factory.create(UserApi::class)
            }
        """.trimIndent())
    }

}
