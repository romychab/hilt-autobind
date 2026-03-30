@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.compiler.HiltComponentResolver
import com.uandcode.hilt.autobind.compiler.METADATA_PACKAGE
import com.uandcode.hilt.autobind.compiler.MetadataInfo
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.findFactoryKType
import com.uandcode.hilt.autobind.compiler.generators.targetsClass
import com.uandcode.hilt.autobind.compiler.kspFail
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
import com.uandcode.hilt.autobind.factories.NoOpBindingFactory
import kotlin.reflect.KClass

internal class AnnotatedSymbolsResolver(
    private val logger: KSPLogger,
    private val componentResolver: HiltComponentResolver,
) {

    private val bindTypesResolver: BindTypesResolver = BindTypesResolver()

    fun processAnnotatedSymbols(
        resolver: Resolver,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
        onGenerateMetaAutoBinding: (MetadataInfo) -> Unit,
    ): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        deferred += processAutoBindsSymbols(resolver, onGenerateHiltModule, onGenerateMetaAutoBinding)
        deferred += processAutoBindsIntoSetSymbols(resolver, onGenerateHiltModule, onGenerateMetaAutoBinding)
        deferred += processMultiModuleMetaAnnotations(resolver, onGenerateHiltModule)
        return deferred
    }

    /**
     * Finds all symbols annotated with [@AutoBinds] and routes them:
     * - Annotation class declarations → treated as meta-annotation aliases (processed via
     *   [processMetaAutoBinds])
     * - All other class declarations → processed normally via [processAutoBinds]
     */
    private fun processAutoBindsSymbols(
        resolver: Resolver,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
        onGenerateMetaAutoBinding: (MetadataInfo) -> Unit,
    ): List<KSAnnotated> = resolver
        .getSymbolsWithAnnotation(requireNotNull(AutoBinds::class.qualifiedName))
        .filterIsInstance<KSClassDeclaration>()
        .collectSymbols { symbol ->
            if (symbol.classKind == ClassKind.ANNOTATION_CLASS) {
                onGenerateMetaAutoBinding(MetadataInfo(symbol))
                processMetaAutoBinds(resolver, symbol, onGenerateHiltModule)
            } else {
                processAutoBinds(
                    annotatedClass = symbol,
                    originAnnotationName = requireNotNull(AutoBinds::class.simpleName),
                    onGenerateHiltModule = onGenerateHiltModule,
                )
                emptyList()
            }
        }

    private fun processMultiModuleMetaAnnotations(
        resolver: Resolver,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
    ): List<KSAnnotated> {
        return resolver.getDeclarationsFromPackage(METADATA_PACKAGE)
            .flatMap { declaration -> declaration.annotations }
            .mapNotNull { annotation ->
                annotation
                    .takeIf { it.shortName.asString() == META_BINDING_INFO }
                    ?.arguments
                    ?.firstOrNull { it.name?.asString() == META_ARG_NAME }
                    ?.value as? String
            }
            .flatMap { annotationQualifiedName ->
                val annotationClassName = resolver
                    .getClassDeclarationByName(annotationQualifiedName)
                    ?: return@flatMap emptyList()
                val symbols = resolver
                    .getSymbolsWithAnnotation(annotationQualifiedName)
                    .filterIsInstance<KSClassDeclaration>()
                    .failOnConflictingAnnotations()
                val hasAutoBindsIntoSet = annotationClassName.annotations
                    .any { it.shortName.asString() == AUTOBINDS_INTO_SET_NAME }
                symbols.collectSymbols { symbol ->
                    if (hasAutoBindsIntoSet) {
                        processAutoBindsIntoSet(
                            annotatedClass = symbol,
                            annotationSource = annotationClassName,
                            originAnnotationName = annotationClassName.simpleName.asString(),
                            onGenerateHiltModule = onGenerateHiltModule,
                        )
                    } else {
                        processAutoBinds(
                            annotatedClass = symbol,
                            annotationSource = annotationClassName,
                            originAnnotationName = annotationClassName.simpleName.asString(),
                            onGenerateHiltModule = onGenerateHiltModule,
                        )
                    }
                    emptyList()
                }
            }
            .toList()
    }

    /**
     * Handles a user-defined annotation class that carries [@AutoBinds] as a meta-annotation.
     *
     * Validates that the annotation targets [AnnotationTarget.CLASS], then finds all classes
     * annotated with it and processes each one using the [@AutoBinds] parameters defined on
     * the meta-annotation declaration.
     */
    private fun processMetaAutoBinds(
        resolver: Resolver,
        metaAnnotation: KSClassDeclaration,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
    ): List<KSAnnotated> {
        if (!metaAnnotation.targetsClass()) {
            kspFail("@AutoBinds meta-annotation '@${metaAnnotation.simpleName.asString()}' must declare " +
                        "@Target(AnnotationTarget.CLASS) to be applied to classes.",
                metaAnnotation,
            )
        }

        val qualifiedName = metaAnnotation.qualifiedName?.asString() ?: return emptyList()

        return resolver
            .getSymbolsWithAnnotation(qualifiedName)
            .filterIsInstance<KSClassDeclaration>()
            .failOnConflictingAnnotations()
            .collectSymbols { symbol ->
                processAutoBinds(
                    annotatedClass = symbol,
                    annotationSource = metaAnnotation,
                    originAnnotationName = metaAnnotation.simpleName.asString(),
                    onGenerateHiltModule = onGenerateHiltModule,
                )
                emptyList()
            }
    }

    /**
     * Finds all symbols annotated with [@AutoBindsIntoSet] and routes them:
     * - Annotation class declarations → treated as meta-annotation aliases (processed via
     *   [processMetaAutoBindsIntoSet])
     * - All other class declarations → processed normally via [processAutoBindsIntoSet]
     */
    private fun processAutoBindsIntoSetSymbols(
        resolver: Resolver,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
        onGenerateMetaAutoBinding: (MetadataInfo) -> Unit,
    ): List<KSAnnotated> = resolver
        .getSymbolsWithAnnotation(requireNotNull(AutoBindsIntoSet::class.qualifiedName))
        .filterIsInstance<KSClassDeclaration>()
        .collectSymbols { symbol ->
            if (symbol.classKind == ClassKind.ANNOTATION_CLASS) {
                onGenerateMetaAutoBinding(MetadataInfo(symbol))
                processMetaAutoBindsIntoSet(resolver, symbol, onGenerateHiltModule)
            } else {
                processAutoBindsIntoSet(
                    annotatedClass = symbol,
                    annotationSource = symbol,
                    originAnnotationName = requireNotNull(AutoBindsIntoSet::class.simpleName),
                    onGenerateHiltModule = onGenerateHiltModule,
                )
                emptyList()
            }
        }

    /**
     * Handles a user-defined annotation class that carries [@AutoBindsIntoSet] as a meta-annotation.
     *
     * Validates that the annotation targets [AnnotationTarget.CLASS], then finds all classes
     * annotated with it and processes each one using the [@AutoBindsIntoSet] parameters defined on
     * the meta-annotation declaration.
     */
    private fun processMetaAutoBindsIntoSet(
        resolver: Resolver,
        metaAnnotation: KSClassDeclaration,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
    ): List<KSAnnotated> {
        if (!metaAnnotation.targetsClass()) {
            kspFail(
                "@AutoBindsIntoSet meta-annotation '@${metaAnnotation.simpleName.asString()}' must declare " +
                        "@Target(AnnotationTarget.CLASS) to be applied to classes.",
                metaAnnotation,
            )
        }

        val qualifiedName = metaAnnotation.qualifiedName?.asString() ?: return emptyList()

        return resolver
            .getSymbolsWithAnnotation(qualifiedName)
            .filterIsInstance<KSClassDeclaration>()
            .failOnConflictingAnnotations()
            .collectSymbols { symbol ->
                processAutoBindsIntoSet(
                    annotatedClass = symbol,
                    annotationSource = metaAnnotation,
                    originAnnotationName = metaAnnotation.simpleName.asString(),
                    onGenerateHiltModule = onGenerateHiltModule,
                )
                emptyList()
            }
    }

    /**
     * Processes a class annotated (directly or via a meta-annotation) with [@AutoBinds].
     *
     * @param annotatedClass The class for which a Hilt module will be generated.
     * @param annotationSource The declaration from which [@AutoBinds] arguments are read.
     *   Defaults to [annotatedClass] for the direct-annotation case; set to the
     *   meta-annotation declaration when the annotation was applied indirectly.
     */
    private fun processAutoBinds(
        annotatedClass: KSClassDeclaration,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
        originAnnotationName: String,
        annotationSource: KSClassDeclaration = annotatedClass,
    ) {
        val annotation = annotationSource
            .getAnnotationsByType(AutoBinds::class)
            .firstOrNull()
        if (annotation == null) {
            kspFail(
                "Can't find AutoBinds annotation for class ${annotatedClass.simpleName}",
                annotatedClass,
            )
        }

        val resolved = componentResolver.resolve(
            declaredComponent = annotation.installIn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
        )

        val bindTargets = bindTypesResolver.findBindToKTypes(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationShortName = AUTOBINDS_NAME,
            originAnnotationName = originAnnotationName,
        )

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            hiltComponentClassName = resolved.hiltComponentClassName,
            scopeClassName = resolved.scopeClassName,
            hasScopeAnnotation = resolved.hasScopeAnnotation,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
            isScopeOnInject = resolved.isScopeOnInject,
            bindTargets = bindTargets,
        )
        val moduleType = getModuleType(annotationSource)
        if (moduleType != null) {
            onGenerateHiltModule.invoke(moduleType, moduleInfo)
        }
    }

    /**
     * Processes a class annotated (directly or via a meta-annotation) with [@AutoBindsIntoSet].
     *
     * @param annotatedClass The class for which a Hilt module will be generated.
     * @param annotationSource The declaration from which [@AutoBindsIntoSet] arguments are read.
     *   Defaults to [annotatedClass] for the direct-annotation case; set to the
     *   meta-annotation declaration when the annotation was applied indirectly.
     */
    private fun processAutoBindsIntoSet(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
    ) {
        val annotation = annotationSource
            .getAnnotationsByType(AutoBindsIntoSet::class)
            .firstOrNull() ?: kspFail(
                "Can't find AutoBindsIntoSet annotation for class ${annotatedClass.simpleName}",
                annotatedClass
            )

        val resolved = componentResolver.resolve(
            declaredComponent = annotation.installIn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
        )

        val bindTargets = bindTypesResolver.findBindToKTypes(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationShortName = AUTOBINDS_INTO_SET_NAME,
            originAnnotationName = originAnnotationName,
        )

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            hiltComponentClassName = resolved.hiltComponentClassName,
            scopeClassName = resolved.scopeClassName,
            isScopeOnInject = resolved.isScopeOnInject,
            annotationSource = annotationSource,
            moduleNameSuffix = "__IntoSetModule",
            hasScopeAnnotation = resolved.hasScopeAnnotation,
            annotationName = originAnnotationName,
            bindTargets = bindTargets,
        )
        onGenerateHiltModule.invoke(ModuleType.IntoSet, moduleInfo)
    }

    /**
     * Determines the module generation strategy by reading the `factory` annotation argument.
     *
     * Note: reading the `factory` argument via raw KSP annotation/arguments API instead of
     * [getAnnotationsByType] because the latter returns a Kotlin [KClass] which cannot be
     * inspected for supertypes. We need the [KSType] to check if the factory implements
     * [ClassBindingFactory].
     *
     * @param annotationSource The declaration that holds the [@AutoBinds] annotation
     *   (either the annotated class itself or a meta-annotation declaration).
     */
    private fun getModuleType(annotationSource: KSClassDeclaration): ModuleType? {
        return findFactoryKType(annotationSource, AUTOBINDS_NAME)
            ?.let { it.declaration as? KSClassDeclaration }
            ?.takeIf { it.qualifiedName?.asString() != NoOpBindingFactory::class.qualifiedName }
            ?.let { factoryDeclaration ->
                val superTypeNames = factoryDeclaration.superTypes
                    .map { it.resolve().declaration.qualifiedName?.asString() }
                    .toList()
                if (ClassBindingFactory::class.qualifiedName in superTypeNames) {
                    ModuleType.ClassFactory(factoryDeclaration)
                } else if (DelegateBindingFactory::class.qualifiedName in superTypeNames) {
                    ModuleType.DelegateFactory(factoryDeclaration)
                } else {
                    logger.error(
                        "AutoBinds Factory class '${factoryDeclaration.simpleName.asString()}' " +
                                "must directly implement ClassBindingFactory or DelegateBindingFactory",
                        factoryDeclaration,
                    )
                    return null
                }
            } ?: ModuleType.Default
    }

    private companion object {
        const val AUTOBINDS_NAME = "AutoBinds"
        const val AUTOBINDS_INTO_SET_NAME = "AutoBindsIntoSet"

        const val META_BINDING_INFO = "MetaAutoBindingInfo"
        const val META_ARG_NAME = "qualifiedMetaAnnotationName"
    }
}
