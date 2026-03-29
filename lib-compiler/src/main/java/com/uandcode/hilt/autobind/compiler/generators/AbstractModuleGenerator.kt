package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.uandcode.hilt.autobind.compiler.ModuleInfo

internal abstract class AbstractModuleGenerator(
    protected val logger: KSPLogger,
) {

    protected fun ModuleInfo.logError(message: String, annotatedClass: KSClassDeclaration) {
        logger.error(
            message = "The class '${annotatedClass.simpleName.asString()}' annotated with " +
                    "@$annotationName annotation $message",
            symbol = annotatedClass,
        )
    }

    protected fun forEachTargetInterface(
        moduleInfo: ModuleInfo,
        targetInterfaces: List<KSType>,
        block: (TargetInterface) -> Unit,
    ) = with(moduleInfo) {
        val classNames = targetInterfaces.mapNotNull { (it.declaration as? KSClassDeclaration)?.toClassName() }
        if (classNames.isEmpty()) {
            logNoImplementedInterfaces(annotatedClass)
            return
        }
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
            block(TargetInterface(functionName, interfaceTypeName))
        }
    }

    protected fun ModuleInfo.logNoImplementedInterfaces(annotatedClass: KSClassDeclaration) {
        logError("must implement at least one interface", annotatedClass)
    }

    class TargetInterface(
        val functionName: String,
        val interfaceTypeName: TypeName,
    )
}
