package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal class BindTypesResolver(
    private val logger: KSPLogger,
) {

    /**
     * Reads the `bindTo` argument from an annotation, returning the list of target types,
     * or null if the argument is absent or empty (meaning auto-detection should be used).
     *
     * Note: `Array<KClass<*>>` annotation arguments are represented as `List<KSType>` in KSP.
     */
    fun findBindToKTypes(
        annotationSource: KSClassDeclaration,
        annotationShortName: String,
    ): List<KSType>? {
        @Suppress("UNCHECKED_CAST")
        val list = annotationSource
            .annotations
            .firstOrNull { it.shortName.asString() == annotationShortName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == BIND_TO_ARG_NAME }
            ?.value as? List<KSType>
        return list?.takeIf { it.isNotEmpty() }
    }

    /**
     * Validates that every type in [bindTargets] is a transitive supertype of [annotatedClass].
     * Emits a compile-time error for each invalid type.
     *
     * @return true if all types are valid, false if any are invalid.
     */
    fun validateBindTargets(
        annotatedClass: KSClassDeclaration,
        bindTargets: List<KSType>,
        annotationName: String,
    ): Boolean {
        val allSuperTypeNames = annotatedClass
            .getAllSuperTypes()
            .mapNotNull { it.declaration.qualifiedName?.asString() }
            .toSet()
        var valid = true
        for (target in bindTargets) {
            val qualifiedName = target.declaration.qualifiedName?.asString()
            if (qualifiedName == null || qualifiedName !in allSuperTypeNames) {
                logger.error(
                    "@$annotationName(bindTo=...): '${target.declaration.simpleName.asString()}' " +
                            "is not a supertype of '${annotatedClass.simpleName.asString()}'.",
                    annotatedClass,
                )
                valid = false
            }
        }
        return valid
    }

}

private const val BIND_TO_ARG_NAME = "bindTo"
