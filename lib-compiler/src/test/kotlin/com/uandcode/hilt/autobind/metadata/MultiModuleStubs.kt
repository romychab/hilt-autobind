package com.uandcode.hilt.autobind.metadata

import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.AutoBindsIntoMap
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.HiltComponent
import com.uandcode.hilt.autobind.MetaAutoBindingInfo
import com.uandcode.hilt.autobind.factories.AutoScoped
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import dagger.multibindings.StringKey
import javax.inject.Inject
import kotlin.reflect.KClass

// This file contains stubs for testing how the library works
// in multi-module environment.

// --- annotations and factories from other module:

@AutoBinds(installIn = HiltComponent.Activity)
@Target(AnnotationTarget.CLASS)
annotation class MultiModuleBindToActivity

@AutoBindsIntoSet(installIn = HiltComponent.Activity)
@Target(AnnotationTarget.CLASS)
annotation class MultiModuleBindIntoSetActivity

@AutoBindsIntoMap(installIn = HiltComponent.Activity)
@StringKey("debug")
@Target(AnnotationTarget.CLASS)
annotation class MultiModuleBindIntoMapActivity

@AutoBinds(factory = MultiModuleFactory::class)
@Target(AnnotationTarget.CLASS)
annotation class MultiModuleFactoryBinding

class MultiModuleFactory @Inject constructor() : ClassBindingFactory {
    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        error("Stub!")
    }
}

// --- auto-generated metadata carriers read by annotation processor

@MetaAutoBindingInfo(
    qualifiedMetaAnnotationName = "com.uandcode.hilt.autobind.metadata.MultiModuleFactoryBinding"
)
class __com__uandcode__hilt__autobind__metadata__MultiModuleFactoryBinding

@MetaAutoBindingInfo(
    qualifiedMetaAnnotationName = "com.uandcode.hilt.autobind.metadata.MultiModuleBindToActivity"
)
class __com__uandcode__hilt__autobind__metadata__MultiModuleBindToActivity

@MetaAutoBindingInfo(
    qualifiedMetaAnnotationName = "com.uandcode.hilt.autobind.metadata.MultiModuleBindIntoSetActivity"
)
class __com__uandcode__hilt__autobind__metadata__MultiModuleBindIntoSetActivity

@MetaAutoBindingInfo(
    qualifiedMetaAnnotationName = "com.uandcode.hilt.autobind.metadata.MultiModuleBindIntoMapActivity"
)
class __com__uandcode__hilt__autobind__metadata__MultiModuleBindIntoMapActivity
