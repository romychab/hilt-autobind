package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.uandcode.hilt.autobind.HiltComponent

/**
 * Resolves the effective [HiltComponent] for an annotated class by combining
 * the declared [HiltComponent] value with the scope annotation on the class.
 *
 * Returns null if validation fails (scope mismatch).
 */
internal class HiltComponentResolver {

    /**
     * Holds the resolved component and scope class names.
     */
    data class Result(
        val hiltComponentClassName: ClassName,
        val scopeClassName: ClassName,
        val hasScopeAnnotation: Boolean,
        val isScopeOnInject: Boolean,
    )

    /**
     * Resolves the effective component for the given [declaredComponent] and [annotatedClass].
     *
     * - If [declaredComponent] is [HiltComponent.Unspecified], auto-detects from the scope
     *   annotation on the class, falling back to [HiltComponent.Singleton].
     * - If [declaredComponent] is explicit and the class has a scope annotation,
     *   validates they match.
     *
     * @return resolved [Result], or null if validation fails.
     */
    fun resolve(
        declaredComponent: HiltComponent,
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        annotationName: String,
    ): Result {
        var classScopeAnnotation = findScopeAnnotation(annotatedClass)
        var isScopeOnInject = false

        if (annotationSource.qualifiedName?.asString() != annotatedClass.qualifiedName?.asString()) {
            val annotationScopeAnnotation = findScopeAnnotation(annotationSource)
            if (classScopeAnnotation == null && annotationScopeAnnotation != null) {
                classScopeAnnotation = annotationScopeAnnotation
                isScopeOnInject = true
            } else if (classScopeAnnotation != annotationScopeAnnotation && annotationScopeAnnotation != null) {
                return handleNonMatchingScopes(annotatedClass, classScopeAnnotation,
                    annotationScopeAnnotation, annotationName)
            }
        }

        return if (declaredComponent == HiltComponent.Unspecified) {
            resolveUnspecifiedComponent(classScopeAnnotation, annotatedClass, annotationName, isScopeOnInject)
        } else if (classScopeAnnotation != null && classScopeAnnotation != declaredComponent.scopeClass) {
            failNonMatchingExplicitComponents(declaredComponent, classScopeAnnotation, annotatedClass, annotationName)
        } else {
            Result(
                hiltComponentClassName = ClassName.bestGuess(declaredComponent.componentClass),
                scopeClassName = ClassName.bestGuess(declaredComponent.scopeClass),
                hasScopeAnnotation = classScopeAnnotation != null,
                isScopeOnInject = isScopeOnInject,
            )
        }
    }

    private fun resolveUnspecifiedComponent(
        classScopeAnnotation: String?,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
        isScopeOnInject: Boolean,
    ): Result {
        val resolved = if (classScopeAnnotation != null) {
            resolveFromScope(classScopeAnnotation, annotatedClass, annotationName)
        } else {
            HiltComponent.Singleton
        }
        return Result(
            hiltComponentClassName = ClassName.bestGuess(resolved.componentClass),
            scopeClassName = ClassName.bestGuess(resolved.scopeClass),
            hasScopeAnnotation = classScopeAnnotation != null,
            isScopeOnInject = isScopeOnInject,
        )
    }

    private fun failNonMatchingExplicitComponents(
        declaredComponent: HiltComponent,
        classScopeAnnotation: String,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): Nothing {
        val scopeSimpleName = classScopeAnnotation.substringAfterLast('.')
        val expectedScopeSimpleName = declaredComponent.scopeClass.substringAfterLast('.')
        kspFail(
            "@$annotationName: class '${annotatedClass.simpleName.asString()}' is scoped " +
                    "with @$scopeSimpleName but installIn targets " +
                    "${declaredComponent.name} (expected scope is @$expectedScopeSimpleName)",
            annotatedClass,
        )
    }

    private fun handleNonMatchingScopes(
        annotatedClass: KSClassDeclaration,
        classScopeAnnotation: String?,
        annotationSourceScopeAnnotation: String?,
        annotationName: String,
    ): Result {
        val classScope = classScopeAnnotation ?: "(Undefined)"
        val sourceScope = annotationSourceScopeAnnotation ?: "(Undefined)"
        kspFail("@$annotationName: class '${annotatedClass.simpleName.asString()}' has " +
                "different scopes. The class is scoped to '$classScope', but the annotation " +
                "@$annotationName targets '$sourceScope'.", annotatedClass)
    }

    /**
     * Finds the fully qualified scope annotation on the class, if any.
     * Checks against all known [HiltComponent] scope classes.
     */
    private fun findScopeAnnotation(annotatedClass: KSClassDeclaration): String? {
        return annotatedClass.annotations
            .map { it.annotationType.resolve().declaration }
            .firstOrNull { annotationDeclaration ->
                annotationDeclaration.annotations.any {
                    it.shortName.asString() == "Scope"
                }
            }
            ?.qualifiedName?.asString()
    }

    /**
     * Resolves the [HiltComponent] from a scope annotation.
     * For `@ViewScoped`, defaults to [HiltComponent.View].
     */
    private fun resolveFromScope(
        scopeQualifiedName: String,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): HiltComponent {
        return HiltComponent.entries.firstOrNull {
            it.scopeClass == scopeQualifiedName
        } ?: kspFail(
            "@$annotationName: unable to resolve Hilt component for scope " +
                    "'${scopeQualifiedName.substringAfterLast('.')}' on class " +
                    "'${annotatedClass.simpleName.asString()}'",
            annotatedClass,
        )
    }

}
