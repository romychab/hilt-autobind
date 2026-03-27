@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import dagger.Binds
import javax.inject.Inject

/**
 * Generates an interface-based Hilt module with `@Binds` functions
 * for each directly implemented interface.
 */
internal class DefaultModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(
        moduleInfo: ModuleInfo,
        targetInterfaces: List<KSType>,
    ): TypeSpec? = with(moduleInfo) {

        if (targetInterfaces.isEmpty()) {
            logNoImplementedInterfaces(annotatedClass)
            return null
        }

        if (annotatedClass.classKind != ClassKind.CLASS) {
            logError("must be a class (not object, interface, etc.)", annotatedClass)
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

        return TypeSpec
            .interfaceBuilder(className = moduleClassName)
            .applyHiltModuleAnnotationsAndModifiers(hiltComponentClassName)
            .apply {
                addBindFunctions(annotatedClass, originClassName, targetInterfaces)
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindFunctions(
        annotatedClass: KSClassDeclaration,
        originClassName: ClassName,
        targetInterfaces: List<KSType>,
    ) = forEachTargetInterface(annotatedClass, targetInterfaces) {
        val funSpec = FunSpec.builder(it.functionName)
            .addParameter(name = "impl", type = originClassName)
            .addAnnotation(Binds::class)
            .addModifiers(KModifier.ABSTRACT)
            .returns(it.interfaceTypeName)
            .build()
        addFunction(funSpec)
    }

}
