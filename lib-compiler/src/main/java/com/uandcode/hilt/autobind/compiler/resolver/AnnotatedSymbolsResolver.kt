@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.compiler.Const.METADATA_PACKAGE
import com.uandcode.hilt.autobind.compiler.Const.META_ARG_NAME
import com.uandcode.hilt.autobind.compiler.Const.META_BINDING_INFO
import com.uandcode.hilt.autobind.compiler.MetadataInfo
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.MetadataGenerator
import com.uandcode.hilt.autobind.compiler.generators.targetsClass
import com.uandcode.hilt.autobind.compiler.kspFail
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import com.uandcode.hilt.autobind.compiler.resolver.collectors.collectSymbols
import com.uandcode.hilt.autobind.compiler.resolver.collectors.failOnConflictingAnnotations

internal class AnnotatedSymbolsResolver(
    hiltModuleGenerator: HiltModuleGenerator,
    private val metadataGenerator: MetadataGenerator,
) {

    val autoBindsResolver = AutoBindsResolver(hiltModuleGenerator)
    val autoBindsIntoSetResolver = AutoBindsIntoSetResolver(hiltModuleGenerator)

    val allResolvers = setOf(
        autoBindsResolver,
        autoBindsIntoSetResolver
    )

    fun processAnnotatedSymbols(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        allResolvers.forEach { autoResolver ->
            deferred += resolve(resolver, autoResolver)
        }
        deferred += resolveMultiModuleMetaAnnotations(resolver)
        return deferred
    }

    private fun resolve(
        resolver: Resolver,
        autoResolver: AutoResolver,
    ): List<KSAnnotated> = resolver
        .getSymbolsWithAnnotation(requireNotNull(autoResolver.annotationClass.qualifiedName))
        .filterIsInstance<KSClassDeclaration>()
        .collectSymbols { symbol ->
            if (symbol.classKind == ClassKind.ANNOTATION_CLASS) {
                metadataGenerator.buildMetadataCarrier(MetadataInfo(symbol))
                processAliases(
                    resolver = resolver,
                    aliasAnnotation = symbol,
                    autoResolver = autoResolver,
                )
            } else {
                autoResolver.resolve(
                    annotatedClass = symbol,
                    annotationSource = symbol,
                    originAnnotationName = requireNotNull(autoResolver.annotationClass.simpleName),
                )
                emptyList()
            }
        }

    private fun resolveMultiModuleMetaAnnotations(
        resolver: Resolver,
    ): List<KSAnnotated> {
        return resolver.getDeclarationsFromPackage(METADATA_PACKAGE)
            .flatMap { declaration -> declaration.annotations }
            .mapNotNull { aliasCandidate ->
                aliasCandidate
                    .takeIf { it.shortName.asString() == META_BINDING_INFO }
                    ?.arguments
                    ?.firstOrNull { it.name?.asString() == META_ARG_NAME }
                    ?.value as? String
            }
            .flatMap { aliasQualifiedName ->
                val aliasAnnotation = resolver
                    .getClassDeclarationByName(aliasQualifiedName)
                    ?: return@flatMap emptyList()
                allResolvers.flatMap { autoResolver ->
                    if (aliasAnnotation.isAnnotationPresent(autoResolver.annotationClass)) {
                        processAliases(resolver, aliasAnnotation, autoResolver)
                    } else {
                        emptyList()
                    }
                }
            }
            .toList()
    }

    private fun processAliases(
        resolver: Resolver,
        aliasAnnotation: KSClassDeclaration,
        autoResolver: AutoResolver,
    ): List<KSAnnotated> {
        if (!aliasAnnotation.targetsClass()) {
            kspFail("Alias '@${aliasAnnotation.simpleName.asString()}' must declare " +
                    "@Target(AnnotationTarget.CLASS) to be applied to classes.",
                aliasAnnotation,
            )
        }
        val qualifiedName = aliasAnnotation.qualifiedName?.asString() ?: return emptyList()

        return resolver
            .getSymbolsWithAnnotation(qualifiedName)
            .filterIsInstance<KSClassDeclaration>()
            .failOnConflictingAnnotations()
            .collectSymbols { symbol ->
                autoResolver.resolve(
                    annotatedClass = symbol,
                    annotationSource = aliasAnnotation,
                    originAnnotationName = aliasAnnotation.simpleName.asString(),
                )
                emptyList()
            }
    }
}
