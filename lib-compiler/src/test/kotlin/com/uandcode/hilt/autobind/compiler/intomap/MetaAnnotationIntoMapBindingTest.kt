package com.uandcode.hilt.autobind.compiler.intomap

import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertCompilationError
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MetaAnnotationIntoMapBindingTest {

    @Test
    fun `alias with baked-in StringKey forwards key to generated function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @StringKey("plugin")
            annotation class ContributeToMap

            @ContributeToMap
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyHandler__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "plugin")
              public fun bindToHandler(`impl`: MyHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `alias without key uses class key`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.IntKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            annotation class ContributeToMap

            @ContributeToMap
            @IntKey(42)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntKey
            import dagger.multibindings.IntoMap

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyHandler__IntoMapModule {
              @Binds
              @IntoMap
              @IntKey(`value` = 42)
              public fun bindToHandler(`impl`: MyHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `alias without key and class without key falls back to ClassKey`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            annotation class ContributeToMap

            @ContributeToMap
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.ClassKey
            import dagger.multibindings.IntoMap

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyHandler__IntoMapModule {
              @Binds
              @IntoMap
              @ClassKey(value = MyHandler::class)
              public fun bindToHandler(`impl`: MyHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `alias and class with same key compiles without conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @StringKey("plugin")
            annotation class ContributeToMap

            @ContributeToMap
            @StringKey("plugin")
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyHandler__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "plugin")
              public fun bindToHandler(`impl`: MyHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `error when alias and class have different keys`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.IntKey
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @StringKey("plugin")
            annotation class ContributeToMap

            @ContributeToMap
            @IntKey(1)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys. Class has @IntKey but alias " +
                "has @StringKey."))
    }

    @Test
    fun `error when alias and class have different keys of same type`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.IntKey
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @StringKey("plugin-1")
            annotation class ContributeToMap

            @ContributeToMap
            @StringKey("plugin-2")
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys: class and alias has keys " +
                "of the same type, but with different values"))
    }

    @Test
    fun `error when class and alias carry Named qualifiers with different values`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject
            import javax.inject.Named

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @Named("api")
            annotation class ContributeToMap

            @ContributeToMap
            @Named("internal")
            @StringKey("plugin-1")
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("has conflicting qualifiers"), result.messages)
    }

    @Test
    fun `qualifier on alias is forwarded to generated Binds function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject
            import javax.inject.Qualifier

            @Qualifier
            @Retention(AnnotationRetention.RUNTIME)
            annotation class DebugHandlers

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @DebugHandlers
            annotation class ContributeDebugHandler

            @ContributeDebugHandler
            @StringKey("main")
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyHandler__IntoMapModule {
              @Binds
              @DebugHandlers
              @IntoMap
              @StringKey(`value` = "main")
              public fun bindToHandler(`impl`: MyHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `scope on alias auto-detects component and forwards scope to binding function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @ActivityScoped
            annotation class ContributeActivityHandler

            @ContributeActivityHandler
            @StringKey("main")
            class MainHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("MainHandler__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.android.components.ActivityComponent
            import dagger.hilt.android.scopes.ActivityScoped
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(ActivityComponent::class)
            internal interface MainHandler__IntoMapModule {
              @Binds
              @ActivityScoped
              @IntoMap
              @StringKey(`value` = "main")
              public fun bindToHandler(`impl`: MainHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `two same custom MapKey annotations with complex values`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import dagger.MapKey
            import javax.inject.Inject

            @MapKey(unwrapValue = false)
            annotation class CustomKey(
                val a: Int,
                val b: String,
            )

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @CustomKey(a = 1, b = "test")
            annotation class Meta

            interface Service

            @Meta
            @CustomKey(b = "test", a = 1)
            class ServiceImpl @Inject constructor() : Service
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("ServiceImpl__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface ServiceImpl__IntoMapModule {
              @Binds
              @IntoMap
              @CustomKey(
                b = "test",
                a = 1,
              )
              public fun bindToService(`impl`: ServiceImpl): Service
            }
        """.trimIndent())
    }

    @Test
    fun `alias and class with same ClassKey compiles without conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.ClassKey
            import javax.inject.Inject

            interface Handler

            class Feature

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @ClassKey(Feature::class)
            annotation class ContributeToMap

            @ContributeToMap
            @ClassKey(Feature::class)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.ClassKey
            import dagger.multibindings.IntoMap

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface MyHandler__IntoMapModule {
              @Binds
              @IntoMap
              @ClassKey(`value` = Feature::class)
              public fun bindToHandler(`impl`: MyHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `error when alias and class have ClassKeys with different classes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.ClassKey
            import javax.inject.Inject

            interface Handler

            class FeatureA
            class FeatureB

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @ClassKey(FeatureA::class)
            annotation class ContributeToMap

            @ContributeToMap
            @ClassKey(FeatureB::class)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys: class and alias has keys " +
                "of the same type, but with different values"), result.messages)
    }

    @Test
    fun `alias and class with same enum MapKey compiles without conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject

            enum class Screen { HOME, SETTINGS }

            @MapKey
            annotation class ScreenKey(val value: Screen)

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @ScreenKey(Screen.HOME)
            annotation class ContributeToMap

            @ContributeToMap
            @ScreenKey(Screen.HOME)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        val generated = result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt")
        assertTrue(generated.contains("@ScreenKey(`value` = Screen.HOME)"), generated)
    }

    @Test
    fun `error when alias and class have enum MapKeys with different entries`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject

            enum class Screen { HOME, SETTINGS }

            @MapKey
            annotation class ScreenKey(val value: Screen)

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @ScreenKey(Screen.HOME)
            annotation class ContributeToMap

            @ContributeToMap
            @ScreenKey(Screen.SETTINGS)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys: class and alias has keys " +
                "of the same type, but with different values"), result.messages)
    }

    @Test
    fun `MapKeys that differ only in an omitted default value do not conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject

            @MapKey(unwrapValue = false)
            annotation class CustomKey(
                val name: String,
                val version: Int = 1,
            )

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @CustomKey("plugin")
            annotation class ContributeToMap

            @ContributeToMap
            @CustomKey("plugin", version = 1)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt")
    }

    @Test
    fun `error when MapKeys differ from an omitted default value`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject

            @MapKey(unwrapValue = false)
            annotation class CustomKey(
                val name: String,
                val version: Int = 1,
            )

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @CustomKey("plugin")
            annotation class ContributeToMap

            @ContributeToMap
            @CustomKey("plugin", version = 2)
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys: class and alias has keys " +
                "of the same type, but with different values"), result.messages)
    }

    @Test
    fun `MapKeys with equal nested annotation and array values do not conflict`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject
            import kotlin.reflect.KClass

            annotation class Tag(val value: String)

            @MapKey(unwrapValue = false)
            annotation class CustomKey(
                val tag: Tag,
                val types: Array<KClass<*>>,
            )

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @CustomKey(tag = Tag("a"), types = [String::class, Int::class])
            annotation class ContributeToMap

            @ContributeToMap
            @CustomKey(types = [String::class, Int::class], tag = Tag("a"))
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()
        result.assertHasGeneratedFile("MyHandler__IntoMapModule.kt")
    }

    @Test
    fun `error when MapKeys have different nested annotation values`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject
            import kotlin.reflect.KClass

            annotation class Tag(val value: String)

            @MapKey(unwrapValue = false)
            annotation class CustomKey(
                val tag: Tag,
                val types: Array<KClass<*>>,
            )

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @CustomKey(tag = Tag("a"), types = [String::class])
            annotation class ContributeToMap

            @ContributeToMap
            @CustomKey(tag = Tag("b"), types = [String::class])
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys: class and alias has keys " +
                "of the same type, but with different values"), result.messages)
    }

    @Test
    fun `error when MapKeys have arrays with different classes`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject
            import kotlin.reflect.KClass

            annotation class Tag(val value: String)

            @MapKey(unwrapValue = false)
            annotation class CustomKey(
                val tag: Tag,
                val types: Array<KClass<*>>,
            )

            interface Handler

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            @CustomKey(tag = Tag("a"), types = [String::class, Int::class])
            annotation class ContributeToMap

            @ContributeToMap
            @CustomKey(tag = Tag("a"), types = [Int::class, String::class])
            class MyHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertCompilationError()
        assertTrue(result.messages.contains("conflicting map keys: class and alias has keys " +
                "of the same type, but with different values"), result.messages)
    }

    @Test
    fun `defining alias only generates metadata carrier file with ContributeToMap in name`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap

            @Target(AnnotationTarget.CLASS)
            @AutoBindsIntoMap
            annotation class ContributeToMap
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generatedNames = result.sourcesGeneratedBySymbolProcessor.map { it.name }
        assertTrue(
            generatedNames.any { it.contains("ContributeToMap") },
            "Expected a metadata file with 'ContributeToMap' in its name, found: $generatedNames",
        )
        result.assertHasGeneratedFile("__test__ContributeToMap.kt").assertContent("""
            package com.uandcode.hilt.autobind.metadata

            import com.uandcode.hilt.autobind.MetaAutoBindingInfo

            @MetaAutoBindingInfo(qualifiedMetaAnnotationName = "test.ContributeToMap")
            internal class __test__ContributeToMap
        """.trimIndent())
    }
}
