package com.uandcode.hilt.autobind.compiler.resolver.collectors

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.uandcode.hilt.autobind.compiler.AutoBindException
import dagger.multibindings.ClassKey

/**
 * Detects the Dagger map key annotation on an annotated class and/or its alias.
 *
 * Detection works by finding any annotation whose own declaration is meta-annotated
 * with `@dagger.MapKey`. This handles all key types uniformly: built-in
 * (`@StringKey`, `@IntKey`, `@LongKey`, `@ClassKey`) and user-defined custom keys.
 *
 * When no key is found on either the class or the alias, falls back to generating
 * `@ClassKey(AnnotatedClass::class)`.
 */
internal class MapKeyCollector {

    /**
     * Resolves the map key annotation spec for the given class and annotation source.
     *
     * When [annotationSource] == [annotatedClass] (direct annotation, no alias), only
     * the class is scanned. When they differ (alias mode), both are scanned and checked
     * for conflicts.
     */
    fun collect(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    ): AnnotationSpec {
        val classKeys = findMapKeyAnnotations(annotatedClass, originAnnotationName)
        val aliasKeys = if (annotationSource != annotatedClass) {
            findMapKeyAnnotations(annotationSource, originAnnotationName)
        } else {
            emptyList()
        }

        val classKey = classKeys.firstOrNull()
        val aliasKey = aliasKeys.firstOrNull()

        return when {
            classKey == null && aliasKey == null -> buildClassKeyFallback(annotatedClass)
            classKey != null && aliasKey == null -> classKey.toAnnotationSpec()
            classKey == null && aliasKey != null -> aliasKey.toAnnotationSpec()
            else -> {
                val finalClassKey = classKey!!
                val finalAliasKey = aliasKey!!
                if (keysAreEqual(finalClassKey, finalAliasKey)) {
                    finalClassKey.toAnnotationSpec()
                } else {
                    if (finalClassKey.shortName.asString() == finalAliasKey.shortName.asString()) {
                        throw AutoBindException(
                            "@$originAnnotationName: conflicting map keys: class and alias has " +
                                    "keys of the same type, but with different values.",
                            annotatedClass,
                        )
                    } else {
                        throw AutoBindException(
                            "@$originAnnotationName: conflicting map keys. Class has " +
                                    "@${finalClassKey.shortName.asString()} but alias " +
                                    "has @${finalAliasKey.shortName.asString()}.",
                            annotatedClass,
                        )
                    }
                }
            }
        }
    }

    private fun findMapKeyAnnotations(
        classDeclaration: KSClassDeclaration,
        originAnnotationName: String,
    ): List<KSAnnotation> {
        val found = classDeclaration.annotations
            .filter { annotation ->
                annotation.annotationType.resolve().declaration
                    .annotations.any { it.shortName.asString() == MAP_KEY_ANNOTATION_NAME }
            }
            .toList()

        if (found.size > 1) {
            throw AutoBindException(
                "@$originAnnotationName: only one @MapKey annotation is allowed, " +
                    "but found: ${found.map { "@${it.shortName.asString()}" }}.",
                classDeclaration,
            )
        }

        return found
    }

    private fun keysAreEqual(a: KSAnnotation, b: KSAnnotation): Boolean {
        if (a.annotationType.resolve() != b.annotationType.resolve()) return false
        if (a.arguments.size != b.arguments.size) return false
        val aArgs = a.arguments.associateBy { it.name?.asString() }
        val bArgs = b.arguments.associateBy { it.name?.asString() }
        return aArgs.keys == bArgs.keys && aArgs.all { (aKey, aValue) ->
            bArgs[aKey]?.value == aValue.value
        }
    }

    private fun buildClassKeyFallback(annotatedClass: KSClassDeclaration): AnnotationSpec {
        return AnnotationSpec.builder(ClassKey::class)
            .addMember("value = %T::class", annotatedClass.toClassName())
            .build()
    }

}

private const val MAP_KEY_ANNOTATION_NAME = "MapKey"
