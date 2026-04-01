package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration

internal data class MetadataInfo(
    val annotatedAnnotation: KSClassDeclaration,
) {
    val qualifiedName: String = requireNotNull(annotatedAnnotation.qualifiedName?.asString())
}
