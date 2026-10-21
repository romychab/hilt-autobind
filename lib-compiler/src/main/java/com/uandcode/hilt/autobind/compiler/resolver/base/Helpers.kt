package com.uandcode.hilt.autobind.compiler.resolver.base

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

internal fun <T : Any> takeIfNoConflicts(
    vararg values: T?,
    onConflict: (List<T>) -> Nothing,
): T? {
    val nonNullValues = values.filterNotNull()
    return if (nonNullValues.isEmpty()) {
        null
    } else if (nonNullValues.all { it == nonNullValues.first() }) {
        nonNullValues.first()
    } else {
        onConflict(nonNullValues.distinct())
    }
}

internal inline fun <reified T : Any> Any.cast(): T? {
    return this as? T
}

/**
 * Returns true if this annotation has the same type and argument values as [other],
 * regardless of argument order and of whether default values are written out.
 */
internal fun KSAnnotation.isSameAnnotationAs(other: KSAnnotation): Boolean {
    return annotationTypeName() == other.annotationTypeName() &&
        argumentValues().let { values ->
            val otherValues = other.argumentValues()
            values.keys == otherValues.keys &&
                values.all { (name, value) -> isSameValue(value, otherValues[name]) }
        }
}

internal fun KSAnnotation.annotationTypeName(): String? =
    annotationType.resolve().declaration.qualifiedName?.asString()

// Defaults go first so that an omitted default equals the same value written out,
// whether or not KSP already lists defaults in `arguments`.
private fun KSAnnotation.argumentValues(): Map<String?, Any?> =
    (defaultArguments + arguments).associate { it.name?.asString() to it.value }

private fun isSameValue(a: Any?, b: Any?): Boolean = when {
    a is KSAnnotation && b is KSAnnotation -> a.isSameAnnotationAs(b)
    a is KSType && b is KSType -> a.declaration.qualifiedName?.asString() == b.declaration.qualifiedName?.asString()
    a is KSClassDeclaration && b is KSClassDeclaration -> a.qualifiedName?.asString() == b.qualifiedName?.asString()
    a is List<*> && b is List<*> -> a.size == b.size && a.indices.all { isSameValue(a[it], b[it]) }
    else -> a == b
}
