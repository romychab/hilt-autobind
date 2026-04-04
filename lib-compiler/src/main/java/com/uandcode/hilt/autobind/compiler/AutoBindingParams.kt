package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ClassName

internal data class AutoBindingParams(
    val hiltComponentClassName: ClassName,
    val hiltScopeClassName: ClassName?,   // null for unscoped custom components
    val isScopedBindingRequired: Boolean,
    val qualifier: KSAnnotation?,
)
