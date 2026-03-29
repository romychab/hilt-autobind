package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration

internal class MetadataInfo(
    val annotatedAnnotation: KSClassDeclaration,
) {
    val qualifiedName: String = requireNotNull(annotatedAnnotation.qualifiedName?.asString())
}
