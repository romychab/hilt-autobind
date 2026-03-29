package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal fun KSClassDeclaration.getTargetInterfaces(): List<KSType> {
    return superTypes
        .map { it.resolve() }
        .filter { (it.declaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE }
        .toList()
}
