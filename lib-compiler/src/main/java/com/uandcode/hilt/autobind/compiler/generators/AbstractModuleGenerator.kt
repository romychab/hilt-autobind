package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.kspFail

internal abstract class AbstractModuleGenerator(
    protected val logger: KSPLogger,
) {

    protected fun ModuleInfo.commonKspFail(message: String, annotatedClass: KSClassDeclaration): Nothing {
        kspFail(
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
            logNoImplementedSuperTypes(annotatedClass)
            return
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

    protected fun ModuleInfo.logNoImplementedSuperTypes(annotatedClass: KSClassDeclaration) {
        commonKspFail("must implement at least one interface or extend a super-class", annotatedClass)
    }

    class TargetSuperType(
        val functionName: String,
        val supertypeTypeName: TypeName,
    )
}
