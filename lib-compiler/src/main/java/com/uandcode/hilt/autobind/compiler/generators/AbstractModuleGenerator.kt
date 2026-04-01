@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.uandcode.hilt.autobind.compiler.AutoBindException
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import javax.inject.Inject

internal abstract class AbstractModuleGenerator(
    protected val logger: KSPLogger,
) {

    protected fun ModuleInfo.commonKspException(message: String, annotatedClass: KSClassDeclaration): Exception {
        return AutoBindException(
            message = "The class '${annotatedClass.simpleName.asString()}' annotated with " +
                    "@$annotationName annotation $message",
            symbol = annotatedClass,
        )
    }

    protected fun forEachTargetSuperType(
        moduleInfo: ModuleInfo,
        targetSuperTypes: List<KSType>,
        block: (TargetSuperType) -> Unit,
    ) = with(moduleInfo) {
        val classNames = targetSuperTypes.mapNotNull { (it.declaration as? KSClassDeclaration)?.toClassName() }
        if (classNames.isEmpty()) {
            throw superTypesNotFoundException(annotatedClass)
        }
        val duplicateSimpleNames = classNames
            .groupBy { it.simpleName }
            .filterValues { it.size > 1 }
            .keys

        for ((index, targetSuperType) in targetSuperTypes.withIndex()) {
            val supertypeClassName = classNames[index]
            val supertypeTypeName = targetSuperType.toTypeName()
            val functionName = if (supertypeClassName.simpleName in duplicateSimpleNames) {
                "bindTo${supertypeClassName.simpleName}_${supertypeClassName.packageName.replace('.', '_')}"
            } else {
                "bindTo${supertypeClassName.simpleName}"
            }
            block(TargetSuperType(functionName, supertypeTypeName))
        }
    }

    protected fun ModuleInfo.superTypesNotFoundException(annotatedClass: KSClassDeclaration) =
        commonKspException("must implement at least one interface or extend a super-class", annotatedClass)

    protected fun FunSpec.Builder.applyQualifier(
        moduleInfo: ModuleInfo,
    ) = apply {
        if (moduleInfo.qualifier != null) {
            addAnnotation(moduleInfo.qualifier.toAnnotationSpec())
        }
    }

    /**
     * @return The list of target super-types for which binding modules
     *   must be generated.
     */
    protected fun ModuleInfo.validateCommonBindingRules(): List<KSType> {
        if (annotatedClass.classKind != ClassKind.CLASS) {
            throw commonKspException("must be a class (not object, interface, etc.)", annotatedClass)
        }

        if (Modifier.INNER in annotatedClass.modifiers) {
            throw commonKspException("must not be an inner class (remove the 'inner' keyword)", annotatedClass)
        }

        val targetSuperTypes = bindTargets ?: annotatedClass.getTargetSuperTypes()
        if (targetSuperTypes.isEmpty()) {
            throw superTypesNotFoundException(annotatedClass)
        }

        if (annotatedClass.isAbstract()) {
            throw commonKspException("must be a non-abstract class", annotatedClass)
        }

        val hasInjectAnnotation = annotatedClass
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            throw commonKspException("must have a primary constructor with @Inject annotation", annotatedClass)
        }

        return targetSuperTypes
    }

    data class TargetSuperType(
        val functionName: String,
        val supertypeTypeName: TypeName,
    )
}
