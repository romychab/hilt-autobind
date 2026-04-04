package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.uandcode.hilt.autobind.HiltComponent
import com.uandcode.hilt.autobind.compiler.Const.QUALIFIER_NAME
import com.uandcode.hilt.autobind.compiler.Const.SCOPE_NAME
import com.uandcode.hilt.autobind.compiler.resolver.base.cast
import com.uandcode.hilt.autobind.compiler.resolver.base.takeIfNoConflicts

internal class AutoBindingParamsResolver(
    private val customComponentResolver: CustomComponentResolver,
) {

    fun resolve(
        installInComponent: HiltComponent,
        installInCustomComponentFqn: String?,
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        annotationName: String,
    ): AutoBindingParams {

        val resolvedHiltComponent = resolveHiltComponent(
            installInComponent = installInComponent,
            installInCustomComponentFqn = installInCustomComponentFqn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = annotationName,
        )

        val classQualifier = takeIfNoConflicts(
            findQualifierAnnotation(annotatedClass),
            findQualifierAnnotation(annotationSource),
            onConflict = { conflictingQualifiers ->
                val conflictingNames = conflictingQualifiers.mapNotNull { it.qualifiedName }
                throw notMatchingQualifiersException(annotatedClass, conflictingNames, annotationName)
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
        installInCustomComponentFqn: String?,
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        annotationName: String,
    ): ResolvedHiltComponent {

        // Conflict: both installIn (enum) and installInCustomComponent specified
        if (installInComponent != HiltComponent.Unspecified && installInCustomComponentFqn != null) {
            throw AutoBindException(
                "@$annotationName: conflicting component specifications: use either " +
                        "'installIn' or 'installInCustomComponent', not both.",
                annotatedClass,
            )
        }

        // Resolve explicit custom component (forward path)
        val explicitCustomCandidate: ComponentCandidate.Custom? =
            installInCustomComponentFqn?.let {
                ComponentCandidate.Custom(
                    customComponentResolver.resolveByComponent(it, annotatedClass, annotationName)
                )
            }

        // Resolve scope annotations on class and alias (standard or reverse-path custom)
        val classCandidate = findComponentFromScope(annotationName, annotatedClass)
        val aliasCandidate = findComponentFromScope(annotationName, annotationSource)

        // Standard enum installIn
        val enumCandidate: ComponentCandidate.Standard? = installInComponent
            .takeIf { it != HiltComponent.Unspecified }
            ?.let { ComponentCandidate.Standard(it) }

        // Conflict detection: all candidates must agree on the component FQN
        val allCandidates = listOfNotNull(enumCandidate, explicitCustomCandidate, classCandidate, aliasCandidate)
        val distinctFqns = allCandidates.map { it.componentFqn }.distinct()
        if (distinctFqns.size > 1) {
            val conflictingScopes = allCandidates.mapNotNull { it.scopeFqn }.distinct()
            throw nonMatchingScopesException(annotatedClass, conflictingScopes, annotationName)
        }

        // isScopedBindingRequired: scope annotation came from the alias annotation class, not the annotated class
        val isScopedBindingRequired = classCandidate == null && aliasCandidate != null

        return when (val winner = allCandidates.firstOrNull()) {
            null -> ResolvedHiltComponent(HiltComponent.Singleton, isScopedBindingRequired = false)
            is ComponentCandidate.Standard -> ResolvedHiltComponent(winner.hiltComponent, isScopedBindingRequired)
            is ComponentCandidate.Custom -> ResolvedHiltComponent(winner.resolved, isScopedBindingRequired)
        }
    }

    private fun findComponentFromScope(
        annotationName: String,
        annotatedClass: KSClassDeclaration,
    ): ComponentCandidate? {
        val scopeAnnotation = annotatedClass.findAnnotation(SCOPE_NAME) ?: return null
        val scopeQualifiedName = scopeAnnotation.qualifiedName ?: return null

        // Try to match against a known standard Hilt component first
        val standardMatch = HiltComponent.entries.firstOrNull {
            it != HiltComponent.Unspecified && it.scopeClass == scopeQualifiedName
        }
        if (standardMatch != null) return ComponentCandidate.Standard(standardMatch)

        // Unknown scope -> reverse path: find the @DefineComponent class with this scope
        val resolved = customComponentResolver.resolveByScope(scopeQualifiedName, annotatedClass, annotationName)
        return ComponentCandidate.Custom(resolved)
    }

    private fun nonMatchingScopesException(
        annotatedClass: KSClassDeclaration,
        conflictingScopes: List<String>,
        annotationName: String,
    ) = AutoBindException(
        "@$annotationName: class has conflicting scopes: $conflictingScopes. " +
                "Make sure you align installIn=... param with Scope annotation.",
        annotatedClass,
    )

    private fun notMatchingQualifiersException(
        annotatedClass: KSClassDeclaration,
        qualifiers: List<String>,
        annotationName: String,
    ) = AutoBindException(
        "@$annotationName: class has conflicting qualifiers: $qualifiers.",
        annotatedClass,
    )

    private fun findQualifierAnnotation(annotatedClass: KSClassDeclaration): ResolvedAnnotation? {
        return annotatedClass.findAnnotation(QUALIFIER_NAME)
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

    private sealed class ComponentCandidate {
        abstract val componentFqn: String
        abstract val scopeFqn: String?

        data class Standard(val hiltComponent: HiltComponent) : ComponentCandidate() {
            override val componentFqn: String get() = hiltComponent.componentClass
            override val scopeFqn: String get() = hiltComponent.scopeClass
        }

        data class Custom(val resolved: ResolvedCustomComponent) : ComponentCandidate() {
            override val componentFqn: String get() = resolved.componentClassName.canonicalName
            override val scopeFqn: String? get() = resolved.scopeClassName?.canonicalName
        }
    }

    private data class ResolvedHiltComponent(
        val componentClassName: ClassName,
        val scopeClassName: ClassName?,
        val isScopedBindingRequired: Boolean,
    ) {
        constructor(hiltComponent: HiltComponent, isScopedBindingRequired: Boolean) : this(
            componentClassName = ClassName.bestGuess(hiltComponent.componentClass),
            scopeClassName = ClassName.bestGuess(hiltComponent.scopeClass),
            isScopedBindingRequired = isScopedBindingRequired,
        )

        constructor(custom: ResolvedCustomComponent, isScopedBindingRequired: Boolean) : this(
            componentClassName = custom.componentClassName,
            scopeClassName = custom.scopeClassName,
            isScopedBindingRequired = isScopedBindingRequired,
        )
    }

    private class ResolvedAnnotation(
        val annotation: KSAnnotation,
        declaration: KSClassDeclaration,
    ) {
        val qualifiedName = declaration.qualifiedName?.asString()

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ResolvedAnnotation) return false
            if (qualifiedName != other.qualifiedName) return false
            return true
        }

        override fun hashCode(): Int = qualifiedName.hashCode()
    }
}
