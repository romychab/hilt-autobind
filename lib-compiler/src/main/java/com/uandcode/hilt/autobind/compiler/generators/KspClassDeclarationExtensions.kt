package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal fun KSClassDeclaration.getTargetSuperTypes(): List<KSType> {
    return superTypes
        .map { it.resolve() }
        .filter { type ->
            val declaration = type.declaration as? KSClassDeclaration ?: return@filter false
            when (declaration.classKind) {
                ClassKind.INTERFACE -> true
                ClassKind.CLASS -> declaration.qualifiedName?.asString() != "kotlin.Any"
                else -> false
            }
        }
        .toList()
}
