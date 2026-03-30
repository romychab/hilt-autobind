package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName

/**
 * Holds naming information for the generated Hilt module.
 */
internal data class ModuleInfo constructor(
    val annotatedClass: KSClassDeclaration,
    val hiltComponentClassName: ClassName,
    val scopeClassName: ClassName,
    val hasScopeAnnotation: Boolean,
    val isScopeOnInject: Boolean,
    val annotationName: String,
    val annotationSource: KSClassDeclaration,
    val moduleNameSuffix: String = "Module",
    val bindTargets: List<KSType>? = null,
) {
    val originClassName: ClassName = annotatedClass.toClassName()
    val transformedClassName: String = originClassName.simpleNames.joinToString("__")
    val moduleClassName: ClassName = ClassName(
        packageName = originClassName.packageName,
        "${transformedClassName}${moduleNameSuffix}",
    )
    val originSimpleName: String = originClassName.simpleName
}
