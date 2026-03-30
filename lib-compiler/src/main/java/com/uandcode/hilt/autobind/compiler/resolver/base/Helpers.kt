package com.uandcode.hilt.autobind.compiler.resolver.base

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
