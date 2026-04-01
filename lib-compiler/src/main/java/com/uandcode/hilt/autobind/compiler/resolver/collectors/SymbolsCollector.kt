@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver.collectors

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.AutoBindsIntoMap
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.compiler.AutoBindException
import kotlin.reflect.KClass

fun Sequence<KSClassDeclaration>.collectSymbols(
    block: (KSClassDeclaration) -> List<KSAnnotated>,
): List<KSAnnotated> {
    val deferred = mutableListOf<KSAnnotated>()
    for (symbol in this) {
        if (!symbol.validate()) {
            deferred.add(symbol)
            continue
        }
        deferred += block(symbol)
    }
    return deferred
}

fun Sequence<KSClassDeclaration>.failOnConflictingAnnotations(): Sequence<KSClassDeclaration> {
    return onEach { declaration ->
        val existingAnnotations = mutableSetOf<KClass<*>>()
        SingletonAnnotations.forEach { kClass ->
            if (declaration.isAnnotationPresent(kClass) && !existingAnnotations.add(kClass)) {
                throw AutoBindException("Annotation '@${kClass.simpleName}' is applied multiple times.", declaration)
            }
            // search aliases
            declaration.annotations
                .map { it.annotationType.resolve().declaration }
                .filterIsInstance<KSClassDeclaration>()
                .forEach {
                    if (it.isAnnotationPresent(kClass) && !existingAnnotations.add(kClass)) {
                        throw AutoBindException("Annotation '@${kClass.simpleName}' is applied multiple times. " +
                                "Review all aliases and exclude duplicated annotations", declaration)
                    }
                }
        }
    }
}

private val SingletonAnnotations = setOf(
    AutoBinds::class,
    AutoBindsIntoSet::class,
    AutoBindsIntoMap::class,
)
