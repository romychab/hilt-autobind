package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.uandcode.hilt.autobind.HiltComponent

/**
 * Resolves the effective [HiltComponent] for an annotated class by combining
 * the declared [HiltComponent] value with the scope annotation on the class.
 *
 * Returns null if validation fails (scope mismatch).
 */
internal class HiltComponentResolver(
    private val logger: KSPLogger,
) {

    /**
     * Holds the resolved component and scope class names.
     */
    data class Result(
        val hiltComponentClassName: ClassName,
        val scopeClassName: ClassName,
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
        annotationName: String,
    ): Result? {
        val classScopeAnnotation = findScopeAnnotation(annotatedClass)

        return if (declaredComponent == HiltComponent.Unspecified) {
            resolveUnspecifiedComponent(classScopeAnnotation, annotatedClass, annotationName)
        } else if (classScopeAnnotation != null && classScopeAnnotation != declaredComponent.scopeClass) {
            handleNonMatchingExplicitComponents(declaredComponent, classScopeAnnotation, annotatedClass, annotationName)
        } else {
            Result(
                hiltComponentClassName = ClassName.bestGuess(declaredComponent.componentClass),
                scopeClassName = ClassName.bestGuess(declaredComponent.scopeClass),
            )
        }
    }

    private fun resolveUnspecifiedComponent(
        classScopeAnnotation: String?,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): Result? {
        val resolved = if (classScopeAnnotation != null) {
            resolveFromScope(classScopeAnnotation, annotatedClass, annotationName) ?: return null
        } else {
            HiltComponent.Singleton
        }
        return Result(
            hiltComponentClassName = ClassName.bestGuess(resolved.componentClass),
            scopeClassName = ClassName.bestGuess(resolved.scopeClass),
        )
    }

    private fun handleNonMatchingExplicitComponents(
        declaredComponent: HiltComponent,
        classScopeAnnotation: String,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): Result? {
        // Explicit component: validate scope match if class has a scope annotation
        val scopeSimpleName = classScopeAnnotation.substringAfterLast('.')
        val expectedScopeSimpleName = declaredComponent.scopeClass.substringAfterLast('.')
        logger.error(
            "@$annotationName: class '${annotatedClass.simpleName.asString()}' is scoped " +
                    "with @$scopeSimpleName but installIn targets " +
                    "${declaredComponent.name} (expected @$expectedScopeSimpleName)",
            annotatedClass,
        )
        return null
    }

    /**
     * Finds the fully qualified scope annotation on the class, if any.
     * Checks against all known [HiltComponent] scope classes.
     */
    private fun findScopeAnnotation(annotatedClass: KSClassDeclaration): String? {
        val classAnnotations = annotatedClass.annotations
            .mapNotNull { it.annotationType.resolve().declaration.qualifiedName?.asString() }
            .toSet()
        return KNOWN_SCOPES.firstOrNull { it in classAnnotations }
    }

    /**
     * Resolves the [HiltComponent] from a scope annotation.
     * For `@ViewScoped`, defaults to [HiltComponent.View].
     */
    private fun resolveFromScope(
        scopeQualifiedName: String,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): HiltComponent? {
        val match = SCOPE_TO_COMPONENT[scopeQualifiedName]
        if (match == null) {
            logger.error(
                "@$annotationName: unable to resolve Hilt component for scope " +
                    "'${scopeQualifiedName.substringAfterLast('.')}' on class " +
                    "'${annotatedClass.simpleName.asString()}'",
                annotatedClass,
            )
            return null
        }
        return match
    }

    private companion object {
        /** Map from scope annotation qualified name to [HiltComponent]. */
        val SCOPE_TO_COMPONENT: Map<String, HiltComponent> = buildMap {
            for (component in HiltComponent.entries) {
                if (component == HiltComponent.Unspecified
                    // @ViewScoped maps to View
                    || component == HiltComponent.ViewWithFragment) continue
                putIfAbsent(component.scopeClass, component)
            }
        }

        val KNOWN_SCOPES: Set<String> = HiltComponent.entries
            .filter { it != HiltComponent.Unspecified }
            .map { it.scopeClass }
            .toSet()
    }
}
