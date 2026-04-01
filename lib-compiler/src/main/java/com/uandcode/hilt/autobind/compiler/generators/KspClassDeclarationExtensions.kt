package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal fun KSClassDeclaration.getTargetSuperTypes(): List<KSType> {
    return superTypes
        .map { it.resolve() }
        .filter { type ->
            val declaration = type.declaration as? KSClassDeclaration
            if (declaration != null) {
                when (declaration.classKind) {
                    ClassKind.INTERFACE -> true
                    ClassKind.CLASS -> declaration.qualifiedName?.asString() != "kotlin.Any"
                    else -> false
                }
            } else {
                false
            }
        }
        .toList()
}

/**
 * Returns true if this annotation declaration has [@Target] that includes
 * [AnnotationTarget.CLASS].
 */
internal fun KSClassDeclaration.targetsClass(): Boolean {
    val targetAnnotation = annotations.firstOrNull {
        it.shortName.asString() == "Target"
    } ?: return false

    @Suppress("UNCHECKED_CAST")
    val targetList = targetAnnotation.arguments
        .firstOrNull()
        ?.value as? List<*>
        ?: return false

    return (targetList.singleOrNull() as? KSClassDeclaration)
        ?.qualifiedName?.asString() == "kotlin.annotation.AnnotationTarget.CLASS"
}

internal fun findFactoryKType(
    annotationSource: KSClassDeclaration,
    annotationName: String,
): KSType? {
    return annotationSource
        .annotations
        .firstOrNull { it.shortName.asString() == annotationName }
        ?.arguments
        ?.firstOrNull { it.name?.asString() == FACTORY_ARG_NAME }
        ?.value as? KSType
}

private const val FACTORY_ARG_NAME = "factory"
