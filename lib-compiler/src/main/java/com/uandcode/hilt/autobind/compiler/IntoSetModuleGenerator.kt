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
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import dagger.Binds
import dagger.multibindings.IntoSet
import javax.inject.Inject

/**
 * Generates an interface-based Hilt module with `@Binds @IntoSet` functions
 * for each directly implemented interface, contributing the class to a
 * multibinding `Set`.
 */
internal class IntoSetModuleGenerator(
    private val logger: KSPLogger,
) {

    fun generate(
        moduleInfo: ModuleInfo,
        targetInterfaces: List<KSType>,
    ): TypeSpec? = with(moduleInfo) {

        if (targetInterfaces.isEmpty()) {
            logError("must implement at least one interface", annotatedClass)
            return null
        }

        if (annotatedClass.classKind != ClassKind.CLASS) {
            logError("must be a class (not object, interface, etc.)", annotatedClass)
            return null
        }

        if (annotatedClass.isAbstract()) {
            logError("must be a final non-abstract class", annotatedClass)
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
                addBindIntoSetFunctions(originClassName, targetInterfaces)
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindIntoSetFunctions(
        originClassName: ClassName,
        targetInterfaces: List<KSType>,
    ) {
        val classNames = targetInterfaces.map { (it.declaration as KSClassDeclaration).toClassName() }
        val duplicateSimpleNames = classNames
            .groupBy { it.simpleName }
            .filterValues { it.size > 1 }
            .keys

        for ((index, targetInterface) in targetInterfaces.withIndex()) {
            val interfaceClassName = classNames[index]
            val interfaceTypeName = targetInterface.toTypeName()
            val functionName = if (interfaceClassName.simpleName in duplicateSimpleNames) {
                "bindTo${interfaceClassName.simpleName}_${interfaceClassName.packageName.replace('.', '_')}"
            } else {
                "bindTo${interfaceClassName.simpleName}"
            }

            val funSpec = FunSpec.builder(functionName)
                .addParameter(name = "impl", type = originClassName)
                .addAnnotation(Binds::class)
                .addAnnotation(IntoSet::class)
                .addModifiers(KModifier.ABSTRACT)
                .returns(interfaceTypeName)
                .build()
            addFunction(funSpec)
        }
    }

    private fun logError(message: String, annotatedClass: KSClassDeclaration) {
        logger.error(
            message = "The class '${annotatedClass.simpleName.asString()}' annotated with " +
                "@AutoBindsIntoSet annotation $message",
            symbol = annotatedClass,
        )
    }
}
