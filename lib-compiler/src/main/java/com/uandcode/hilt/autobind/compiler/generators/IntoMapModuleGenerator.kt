@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import dagger.multibindings.IntoMap

/**
 * Generates an interface-based Hilt module with `@Binds @IntoMap` functions
 * for each directly implemented interface, contributing the class to a
 * multibinding `Map`.
 */
internal class IntoMapModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger = logger) {

    fun generate(
        moduleInfo: ModuleInfo,
        mapKeyAnnotationSpec: AnnotationSpec,
        isObject: Boolean,
    ): TypeSpec = with(moduleInfo) {
        val targetSuperTypes = validateCommonBindingRules()
        return createTypeSpecBuilder(isObject)
            .preBuildHiltModuleTypeSpec(hiltComponentClassName)
            .apply {
                addBindIntoMapFunctions(moduleInfo, originClassName, targetSuperTypes, mapKeyAnnotationSpec, isObject)
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindIntoMapFunctions(
        moduleInfo: ModuleInfo,
        originClassName: ClassName,
        targetSuperTypes: List<KSType>,
        mapKeyAnnotationSpec: AnnotationSpec,
        isObject: Boolean,
    ) = forEachTargetSuperType(moduleInfo, targetSuperTypes) { targetSuperType ->
        preBuildBinder(moduleInfo, targetSuperType, originClassName, isObject)
            .addAnnotation(IntoMap::class)
            .addAnnotation(mapKeyAnnotationSpec)
            .build()
            .let(::addFunction)
    }

}
