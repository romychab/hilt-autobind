package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSAnnotation
import com.squareup.kotlinpoet.ClassName

data class AutoBindingParams(
    val hiltComponentClassName: ClassName,
    val hiltScopeClassName: ClassName,
    val isScopedBindingRequired: Boolean,
    val qualifier: KSAnnotation?,
)
