@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.uandcode.hilt.autobind.compiler.Const.DEFINE_COMPONENT_NAME
import com.uandcode.hilt.autobind.compiler.Const.SCOPE_NAME

/**
 * Resolves custom Hilt components (defined with `@DefineComponent`) from two directions:
 * - Forward: given a component class FQN, finds its scope annotation.
 * - Reverse: given a scope annotation FQN, finds the `@DefineComponent` class annotated with it.
 */
internal class CustomComponentResolver(
    private val resolver: Resolver,
) {

    private val cache = mutableMapOf<String, ResolvedCustomComponent>()

    /**
     * Forward path: resolves a custom component by its fully-qualified class name.
     * Finds the scope annotation declared on the component class (if any).
     */
    fun resolveByComponent(
        componentFqn: String,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): ResolvedCustomComponent {
        return cache.getOrPut(componentFqn) {
            val componentDeclaration = resolver.getClassDeclarationByName(componentFqn)
                ?: throw AutoBindException(
                    "@$annotationName: installInCustomComponent class '$componentFqn' " +
                            "was not found in the classpath.",
                    annotatedClass,
                )
            val scopeDeclaration = componentDeclaration.findScopeAnnotationDeclaration()
            ResolvedCustomComponent(
                componentClassName = componentDeclaration.toClassName(),
                scopeClassName = scopeDeclaration?.toClassName(),
            )
        }
    }

    /**
     * Reverse path: finds the `@DefineComponent` class annotated with the given scope FQN.
     * Returns the resolved component + its scope class name (which equals [scopeFqn]).
     */
    fun resolveByScope(
        scopeFqn: String,
        annotatedClass: KSClassDeclaration,
        annotationName: String,
    ): ResolvedCustomComponent {
        return cache.getOrPut(scopeFqn) {
            val candidates = resolver
                .getSymbolsWithAnnotation(scopeFqn)
                .filterIsInstance<KSClassDeclaration>()
                .filter { it.hasDefineComponentAnnotation() }
                .toList()

            when {
                candidates.isEmpty() -> throw AutoBindException(
                    "@$annotationName: unable to resolve Hilt component for scope " +
                            "'${scopeFqn.substringAfterLast('.')}' on class " +
                            "'${annotatedClass.simpleName.asString()}'. " +
                            "No @DefineComponent class found with this scope.",
                    annotatedClass,
                )
                candidates.size > 1 -> throw AutoBindException(
                    "@$annotationName: ambiguous scope '${scopeFqn.substringAfterLast('.')}' " +
                            "on class '${annotatedClass.simpleName.asString()}': multiple " +
                            "@DefineComponent classes carry this scope: " +
                            "${candidates.mapNotNull { it.qualifiedName?.asString() }}.",
                    annotatedClass,
                )
                else -> {
                    val componentDeclaration = candidates.single()
                    ResolvedCustomComponent(
                        componentClassName = componentDeclaration.toClassName(),
                        scopeClassName = componentDeclaration
                            .findScopeAnnotationDeclaration()
                            ?.toClassName(),
                    )
                }
            }
        }
    }

    private fun KSClassDeclaration.hasDefineComponentAnnotation(): Boolean {
        return annotations.any { it.shortName.asString() == DEFINE_COMPONENT_NAME }
    }

    private fun KSClassDeclaration.findScopeAnnotationDeclaration(): KSClassDeclaration? {
        return annotations
            .firstNotNullOfOrNull { annotation ->
                (annotation.annotationType.resolve().declaration as? KSClassDeclaration)
                    ?.takeIf { annotationDeclaration ->
                        annotationDeclaration.annotations.any {
                            it.shortName.asString() == SCOPE_NAME
                        }
                    }
            }
    }
}

internal data class ResolvedCustomComponent(
    val componentClassName: ClassName,
    val scopeClassName: ClassName?,   // null = unscoped component
)
