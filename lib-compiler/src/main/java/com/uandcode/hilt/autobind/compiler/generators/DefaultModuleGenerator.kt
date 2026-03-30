@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import dagger.Binds
import dagger.Provides
import javax.inject.Inject

/**
 * Generates an interface-based Hilt module with `@Binds` functions
 * for each directly implemented interface.
 */
internal class DefaultModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(moduleInfo: ModuleInfo): TypeSpec? = with(moduleInfo) {
        if (annotatedClass.classKind != ClassKind.CLASS) {
            logError("must be a class (not object, interface, etc.)", annotatedClass)
            return null
        }

        if (Modifier.INNER in annotatedClass.modifiers) {
            logError("must not be an inner class (remove the 'inner' keyword)", annotatedClass)
            return null
        }

        val targetSuperTypes = annotatedClass.getTargetSuperTypes()
        if (targetSuperTypes.isEmpty()) {
            logNoImplementedSuperTypes(annotatedClass)
            return null
        }

        if (annotatedClass.isAbstract()) {
            logError("must be a non-abstract class", annotatedClass)
            return null
        }

        val hasInjectAnnotation = annotatedClass
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            logError("must have a primary constructor with @Inject annotation", annotatedClass)
            return null
        }

        val typeSpecHeader = if (isObjectModuleRequired) {
            TypeSpec.objectBuilder(className = moduleClassName)
        } else {
            TypeSpec.interfaceBuilder(className = moduleClassName)
        }

        return typeSpecHeader
            .applyHiltModuleAnnotationsAndModifiers(hiltComponentClassName)
            .apply {
                if (isObjectModuleRequired) {
                    addProvideFunctions(moduleInfo, originClassName, targetSuperTypes)
                } else {
                    addBindFunctions(moduleInfo, originClassName, targetSuperTypes)
                }
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindFunctions(
        moduleInfo: ModuleInfo,
        originClassName: ClassName,
        targetSuperTypes: List<KSType>,
    ) = forEachTargetSuperType(moduleInfo, targetSuperTypes) {
        val funSpec = FunSpec.builder(it.functionName)
            .addParameter(name = "impl", type = originClassName)
            .addAnnotation(Binds::class)
            .addModifiers(KModifier.ABSTRACT)
            .returns(it.supertypeTypeName)
            .build()
        addFunction(funSpec)
    }

    private fun TypeSpec.Builder.addProvideFunctions(
        moduleInfo: ModuleInfo,
        originClassName: ClassName,
        targetSuperTypes: List<KSType>,
    ) = forEachTargetSuperType(moduleInfo, targetSuperTypes) {
        val funSpec = FunSpec.builder(it.functionName)
            .addParameter(name = "impl", type = originClassName)
            .addAnnotation(Provides::class)
            .apply {
                if (moduleInfo.hasScopeAnnotation && moduleInfo.isObjectModuleRequired) {
                    addAnnotation(moduleInfo.scopeClassName)
                }
            }
            .addCode("return impl")
            .returns(it.supertypeTypeName)
            .build()
        addFunction(funSpec)
    }

}
