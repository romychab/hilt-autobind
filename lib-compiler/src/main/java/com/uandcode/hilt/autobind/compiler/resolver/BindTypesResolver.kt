package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.uandcode.hilt.autobind.compiler.kspFail

internal class BindTypesResolver {

    /**
     * Reads the `bindTo` argument from an annotation, returning the list of target types,
     * or null if the argument is absent or empty (meaning auto-detection should be used).
     *
     * Note: `Array<KClass<*>>` annotation arguments are represented as `List<KSType>` in KSP.
     */
    fun findBindToKTypes(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        annotationShortName: String,
        originAnnotationName: String,
    ): List<KSType>? {
        @Suppress("UNCHECKED_CAST")
        val list = annotationSource
            .annotations
            .firstOrNull { it.shortName.asString() == annotationShortName }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == BIND_TO_ARG_NAME }
            ?.value as? List<KSType>
        return list?.takeIf { it.isNotEmpty() }
            ?.also {
                validateBindTargets(annotatedClass, it, originAnnotationName)
            }
    }

    /**
     * Validates that every type in [bindTargets] is a transitive supertype of [annotatedClass].
     * Emits a compile-time error for each invalid type.
     */
    private fun validateBindTargets(
        annotatedClass: KSClassDeclaration,
        bindTargets: List<KSType>,
        annotationName: String,
    ) {
        val allSuperTypeNames = annotatedClass
            .getAllSuperTypes()
            .mapNotNull { it.declaration.qualifiedName?.asString() }
            .toSet()
        for (target in bindTargets) {
            val qualifiedName = target.declaration.qualifiedName?.asString()
            if (qualifiedName == null || qualifiedName !in allSuperTypeNames) {
                kspFail(
                    "@$annotationName(bindTo=...): '${target.declaration.simpleName.asString()}' " +
                            "is not a supertype of '${annotatedClass.simpleName.asString()}'.",
                    annotatedClass,
                )
            }
        }
    }

}

private const val BIND_TO_ARG_NAME = "bindTo"
