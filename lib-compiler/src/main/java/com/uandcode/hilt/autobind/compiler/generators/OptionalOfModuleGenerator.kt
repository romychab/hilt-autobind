@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import dagger.BindsOptionalOf

/**
 * Generates an interface-based Hilt module with a single `@BindsOptionalOf`
 * declaration for the annotated type.
 *
 * Dagger forbids scoped `@BindsOptionalOf` declarations, so [ModuleInfo.isScopedBindingRequired]
 * is deliberately ignored here: a scope annotation only selects the target component.
 */
internal class OptionalOfModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(
        moduleInfo: ModuleInfo,
    ): TypeSpec = with(moduleInfo) {
        validateOptionalOfRules()
        return createTypeSpecBuilder(isObject = false)
            .preBuildHiltModuleTypeSpec(hiltComponentClassName)
            .addFunction(buildOptionalOfFunction(moduleInfo))
            .build()
    }

    private fun buildOptionalOfFunction(
        moduleInfo: ModuleInfo,
    ): FunSpec = FunSpec.builder("optional${moduleInfo.originSimpleName}")
        .addAnnotation(BindsOptionalOf::class)
        .addModifiers(KModifier.ABSTRACT)
        .applyQualifier(moduleInfo)
        .returns(moduleInfo.originClassName)
        .build()

    private fun ModuleInfo.validateOptionalOfRules() {
        val isInterface = annotatedClass.classKind == ClassKind.INTERFACE
        val isAbstractClass = annotatedClass.classKind == ClassKind.CLASS && annotatedClass.isAbstract()
        if (!isInterface && !isAbstractClass) {
            throw commonKspException(
                "must be applied to an interface or abstract class. Did you mean @AutoBinds?",
                annotatedClass,
            )
        }
        if (Modifier.INNER in annotatedClass.modifiers) {
            throw commonKspException("must not be an inner class (remove the 'inner' keyword)", annotatedClass)
        }
        if (annotatedClass.typeParameters.isNotEmpty()) {
            throw commonKspException("must not have type parameters", annotatedClass)
        }
    }
}
