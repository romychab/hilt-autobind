package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.uandcode.hilt.autobind.HiltComponent
import com.uandcode.hilt.autobind.compiler.resolver.base.cast
import com.uandcode.hilt.autobind.compiler.resolver.base.takeIfNoConflicts


internal class AutoBindingParamsResolver {

    fun resolve(
        installInComponent: HiltComponent,
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        annotationName: String,
    ): AutoBindingParams {

        val resolvedHiltComponent = resolveHiltComponent(
            installInComponent = installInComponent,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = annotationName,
        )

        val classQualifier = takeIfNoConflicts(
            findQualifierAnnotation(annotatedClass),
            findQualifierAnnotation(annotationSource),
            onConflict = { conflictingQualifiers ->
                val conflictingNames = conflictingQualifiers.mapNotNull { it.qualifiedName }
                failNotMatchingQualifiers(annotatedClass, conflictingNames, annotationName)
            }
        )

        return AutoBindingParams(
            hiltComponentClassName = resolvedHiltComponent.componentClassName,
            hiltScopeClassName = resolvedHiltComponent.scopeClassName,
            isScopedBindingRequired = resolvedHiltComponent.isScopedBindingRequired,
            qualifier = classQualifier?.annotation,
        )
    }

    private fun resolveHiltComponent(
        installInComponent: HiltComponent,
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        annotationName: String,
    ): ResolvedHiltComponent {
        val classHiltComponent = findScopeAnnotation(annotationName, annotatedClass)
        val aliasHiltComponent = findScopeAnnotation(annotationName, annotationSource)

        val hiltComponent = takeIfNoConflicts(
            installInComponent.takeIf { it != HiltComponent.Unspecified },
            classHiltComponent,
            aliasHiltComponent,
            onConflict = { conflictingComponents ->
                val conflictingScopes = conflictingComponents.map { it.scopeClass }
                failNonMatchingScopes(annotatedClass, conflictingScopes, annotationName)
            }
        ) ?: HiltComponent.Singleton

        return ResolvedHiltComponent(
            hiltComponent = hiltComponent,
            isScopedBindingRequired = classHiltComponent == null && aliasHiltComponent != null,
        )
    }

    private fun failNonMatchingScopes(
        annotatedClass: KSClassDeclaration,
        conflictingScopes: List<String>,
        annotationName: String,
    ): Nothing {
        kspFail("@$annotationName: class has conflicting scopes: $conflictingScopes. " +
                "Make sure you align installIn=... param with Scope annotation.", annotatedClass)
    }

    private fun failNotMatchingQualifiers(
        annotatedClass: KSClassDeclaration,
        qualifiers: List<String>,
        annotationName: String,
    ): Nothing {
        kspFail("@$annotationName: class has conflicting qualifiers: $qualifiers.", annotatedClass)
    }

    /**
     * Finds the fully qualified scope annotation on the class, if any.
     * Checks against all known [HiltComponent] scope classes.
     */
    private fun findScopeAnnotation(
        annotationName: String,
        annotatedClass: KSClassDeclaration,
    ): HiltComponent? {
        return annotatedClass.findAnnotation("Scope")
            ?.qualifiedName
            ?.let { scopeQualifiedName ->
                HiltComponent.entries.firstOrNull {
                    it.scopeClass == scopeQualifiedName
                } ?: kspFail(
                    "@$annotationName: unable to resolve Hilt component for scope " +
                            "'${scopeQualifiedName.substringAfterLast('.')}' on class " +
                            "'${annotatedClass.simpleName.asString()}'",
                    annotatedClass,
                )
            }
    }

    private fun findQualifierAnnotation(annotatedClass: KSClassDeclaration): ResolvedAnnotation? {
        return annotatedClass.findAnnotation("Qualifier")
    }

    private fun KSClassDeclaration.findAnnotation(shortTypeName: String): ResolvedAnnotation? {
        return annotations
            .firstNotNullOfOrNull { annotation ->
                annotation.annotationType.resolve().declaration
                    .takeIf { annotationDeclaration ->
                        annotationDeclaration.annotations.any {
                            it.shortName.asString() == shortTypeName
                        }
                    }
                    ?.cast<KSClassDeclaration>()
                    ?.let { ResolvedAnnotation(annotation, it) }
            }
    }

    private class ResolvedAnnotation(
        val annotation: KSAnnotation,
        val declaration: KSClassDeclaration,
    ) {
        val qualifiedName = declaration.qualifiedName?.asString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ResolvedAnnotation) return false
            if (qualifiedName != other.qualifiedName) return false
            return true
        }

        override fun hashCode(): Int {
            return qualifiedName.hashCode()
        }
    }

    private class ResolvedHiltComponent(
        val scopeClassName: ClassName,
        val componentClassName: ClassName,
        val isScopedBindingRequired: Boolean,
    ) {
        constructor(
            hiltComponent: HiltComponent,
            isScopedBindingRequired: Boolean,
        ) : this(
            scopeClassName = ClassName.bestGuess(hiltComponent.scopeClass),
            componentClassName = ClassName.bestGuess(hiltComponent.componentClass),
            isScopedBindingRequired = isScopedBindingRequired,
        )
    }
}
