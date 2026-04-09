package com.uandcode.hilt.autobind.compiler.intomap

import com.tschuchort.compiletesting.SourceFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertContent
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertHasGeneratedFile
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.assertOk
import com.uandcode.hilt.autobind.compiler.CompilationTestHelper.compile
import org.junit.jupiter.api.Test

class IntoMapBindingTest {

    @Test
    fun `basic StringKey generates __IntoMapModule with Binds IntoMap StringKey`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoMap
            @StringKey("logging")
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
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
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `IntKey generates __IntoMapModule with Binds IntoMap IntKey`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.IntKey
            import javax.inject.Inject

            interface Handler

            @AutoBindsIntoMap
            @IntKey(42)
            class DefaultHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("DefaultHandler__IntoMapModule.kt")
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
            internal interface DefaultHandler__IntoMapModule {
              @Binds
              @IntoMap
              @IntKey(`value` = 42)
              public fun bindToHandler(`impl`: DefaultHandler): Handler
            }
        """.trimIndent())
    }

    @Test
    fun `custom MapKey annotation generates __IntoMapModule`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.MapKey
            import javax.inject.Inject

            interface Service

            enum class ServiceType { PRIMARY }

            @MapKey
            annotation class ServiceTypeKey(val value: ServiceType)

            @AutoBindsIntoMap
            @ServiceTypeKey(ServiceType.PRIMARY)
            class PrimaryService @Inject constructor() : Service
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("PrimaryService__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface PrimaryService__IntoMapModule {
              @Binds
              @IntoMap
              @ServiceTypeKey(`value` = ServiceType.PRIMARY)
              public fun bindToService(`impl`: PrimaryService): Service
            }
        """.trimIndent())
    }

    @Test
    fun `no key annotation falls back to ClassKey`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoMap
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
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
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @ClassKey(value = LoggingInterceptor::class)
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `multiple supertypes generates binding for each supertype`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor
            interface Closeable

            @AutoBindsIntoMap
            @StringKey("logging")
            class LoggingInterceptor @Inject constructor() : Interceptor, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
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
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor

              @Binds
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToCloseable(`impl`: LoggingInterceptor): Closeable
            }
        """.trimIndent())
    }

    @Test
    fun `bindTo restricts binding targets to listed supertypes only`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor
            interface Closeable

            @AutoBindsIntoMap(bindTo = [Interceptor::class])
            @StringKey("x")
            class LoggingInterceptor @Inject constructor() : Interceptor, Closeable
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
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
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "x")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `Named qualifier is forwarded to generated Binds function`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject
            import javax.inject.Named

            interface Interceptor

            @Named("debug")
            @AutoBindsIntoMap
            @StringKey("logging")
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt")
        generated.assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Named

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @Named(`value` = "debug")
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `scope annotation on alias auto-detects component and forwards scope`() {
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
            annotation class ContributeActivityScopedHandler

            @ContributeActivityScopedHandler
            @StringKey("main")
            class MainHandler @Inject constructor() : Handler
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        val generated = result.assertHasGeneratedFile("MainHandler__IntoMapModule.kt")
        generated.assertContent("""
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
    fun `AutoBinds and AutoBindsIntoMap on same class generate separate modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor

            @AutoBinds
            @AutoBindsIntoMap
            @StringKey("logging")
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptorModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptorModule {
              @Binds
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
        result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `AutoBindsIntoSet and AutoBindsIntoMap on same class generate separate modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor

            @AutoBindsIntoSet
            @AutoBindsIntoMap
            @StringKey("logging")
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
        result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

    @Test
    fun `all three AutoBinds AutoBindsIntoSet AutoBindsIntoMap generate all three modules`() {
        val source = SourceFile.kotlin("Test.kt", """
            package test

            import com.uandcode.hilt.autobind.AutoBinds
            import com.uandcode.hilt.autobind.AutoBindsIntoSet
            import com.uandcode.hilt.autobind.AutoBindsIntoMap
            import dagger.multibindings.StringKey
            import javax.inject.Inject

            interface Interceptor

            @AutoBinds
            @AutoBindsIntoSet
            @AutoBindsIntoMap
            @StringKey("logging")
            class LoggingInterceptor @Inject constructor() : Interceptor
        """.trimIndent())

        val result = compile(source)
        result.assertOk()

        result.assertHasGeneratedFile("LoggingInterceptorModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptorModule {
              @Binds
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
        result.assertHasGeneratedFile("LoggingInterceptor__IntoSetModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoSet

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoSetModule {
              @Binds
              @IntoSet
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
        result.assertHasGeneratedFile("LoggingInterceptor__IntoMapModule.kt").assertContent("""
            package test

            import dagger.Binds
            import dagger.Module
            import dagger.hilt.InstallIn
            import dagger.hilt.components.SingletonComponent
            import dagger.multibindings.IntoMap
            import dagger.multibindings.StringKey

            @Module
            @InstallIn(SingletonComponent::class)
            internal interface LoggingInterceptor__IntoMapModule {
              @Binds
              @IntoMap
              @StringKey(`value` = "logging")
              public fun bindToInterceptor(`impl`: LoggingInterceptor): Interceptor
            }
        """.trimIndent())
    }

}
