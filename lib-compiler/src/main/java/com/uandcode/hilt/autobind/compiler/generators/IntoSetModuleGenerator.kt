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
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Generates an interface-based Hilt module with `@Binds @IntoSet` functions
 * for each directly implemented interface, contributing the class to a
 * multibinding `Set`.
 */
internal class IntoSetModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger = logger) {

    fun generate(moduleInfo: ModuleInfo): TypeSpec = with(moduleInfo) {

        if (annotatedClass.classKind != ClassKind.CLASS) {
            commonKspFail("must be a class (not object, interface, etc.)", annotatedClass)
        }

        if (Modifier.INNER in annotatedClass.modifiers) {
            commonKspFail("must not be an inner class (remove the 'inner' keyword)", annotatedClass)
        }

        val targetSuperTypes = moduleInfo.bindTargets ?: annotatedClass.getTargetSuperTypes()
        if (targetSuperTypes.isEmpty()) {
            logNoImplementedSuperTypes(annotatedClass)
        }

        if (annotatedClass.isAbstract()) {
            commonKspFail("must be a final non-abstract class", annotatedClass)
        }

        val hasInjectAnnotation = annotatedClass
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            commonKspFail("must have a primary constructor with @Inject annotation", annotatedClass)
        }

        return TypeSpec
            .interfaceBuilder(className = moduleClassName)
            .applyHiltModuleAnnotationsAndModifiers(hiltComponentClassName)
            .apply {
                addBindIntoSetFunctions(moduleInfo, originClassName, targetSuperTypes)
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindIntoSetFunctions(
        moduleInfo: ModuleInfo,
        originClassName: ClassName,
        targetSuperTypes: List<KSType>,
    ) = forEachTargetSuperType(moduleInfo, targetSuperTypes) {
        val funSpec = FunSpec.builder(it.functionName)
            .addParameter(name = "impl", type = originClassName)
            .addAnnotation(Binds::class)
            .addAnnotation(IntoSet::class)
            .apply {
                if (moduleInfo.hasScopeAnnotation) {
                    addAnnotation(moduleInfo.scopeClassName)
                }
            }
            .addModifiers(KModifier.ABSTRACT)
            .returns(it.supertypeTypeName)
            .build()
        addFunction(funSpec)
    }

}
