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
import com.google.devtools.ksp.validate
import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.compiler.HiltComponentResolver
import com.uandcode.hilt.autobind.compiler.METADATA_PACKAGE
import com.uandcode.hilt.autobind.compiler.MetadataInfo
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
import com.uandcode.hilt.autobind.factories.NoOpBindingFactory
import kotlin.reflect.KClass

internal class AnnotatedSymbolsResolver(
    private val logger: KSPLogger,
    private val componentResolver: HiltComponentResolver,
) {

    private val bindTypesResolver: BindTypesResolver = BindTypesResolver(logger)

    fun processAnnotatedSymbols(
        resolver: Resolver,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
        onGenerateMetaAutoBinding: (MetadataInfo) -> Unit,
    ): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        deferred += processAutoBindsSymbols(resolver, onGenerateHiltModule, onGenerateMetaAutoBinding)
        deferred += processAutoBindsIntoSet(resolver, onGenerateHiltModule)
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
    ): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(requireNotNull(AutoBinds::class.qualifiedName))
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val deferred = mutableListOf<KSAnnotated>()
        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred.add(symbol)
                continue
            }
            if (symbol.classKind == ClassKind.ANNOTATION_CLASS) {
                onGenerateMetaAutoBinding(MetadataInfo(symbol))
                deferred += processMetaAutoBinds(resolver, symbol, onGenerateHiltModule)
            } else {
                processAutoBinds(
                    annotatedClass = symbol,
                    originAnnotationName = requireNotNull(AutoBinds::class.simpleName),
                    onGenerateHiltModule = onGenerateHiltModule,
                )
            }
        }
        return deferred
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
                    .toList()
                val deferred = mutableListOf<KSAnnotated>()
                for (symbol in symbols) {
                    if (!symbol.validate()) {
                        deferred.add(symbol)
                        continue
                    }
                    processAutoBinds(
                        annotatedClass = symbol,
                        annotationSource = annotationClassName,
                        originAnnotationName = annotationClassName.simpleName.asString(),
                        onGenerateHiltModule = onGenerateHiltModule,
                    )
                }
                deferred
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
            logger.error(
                "@AutoBinds meta-annotation '@${metaAnnotation.simpleName.asString()}' must declare " +
                        "@Target(AnnotationTarget.CLASS) to be applied to classes.",
                metaAnnotation,
            )
            return emptyList()
        }

        val qualifiedName = metaAnnotation.qualifiedName?.asString() ?: return emptyList()
        val deferred = mutableListOf<KSAnnotated>()

        val symbols = resolver
            .getSymbolsWithAnnotation(qualifiedName)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred.add(symbol)
                continue
            }
            processAutoBinds(
                annotatedClass = symbol,
                annotationSource = metaAnnotation,
                originAnnotationName = metaAnnotation.simpleName.asString(),
                onGenerateHiltModule = onGenerateHiltModule,
            )
        }
        return deferred
    }

    private fun processAutoBindsIntoSet(
        resolver: Resolver,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
    ): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(requireNotNull(AutoBindsIntoSet::class.qualifiedName))
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val deferred = mutableListOf<KSAnnotated>()
        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred.add(symbol)
                continue
            }
            processAutoBindsIntoSet(symbol, onGenerateHiltModule)
        }
        return deferred
    }

    /**
     * Processes a class annotated (directly or via a meta-annotation) with [@AutoBinds].
     *
     * @param annotatedClass The class for which a Hilt module will be generated.
     * @param annotationSource The declaration from which [@AutoBinds] arguments are read.
     *   Defaults to [annotatedClass] for the direct-annotation case; set to the
     *   meta-annotation declaration when the annotation was applied indirectly.
     */
    @Suppress("ReturnCount")
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
            logger.error(
                "Can't find AutoBinds annotation for class ${annotatedClass.simpleName}",
                annotatedClass,
            )
            return
        }

        val resolved = componentResolver.resolve(
            declaredComponent = annotation.installIn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
        ) ?: return

        val bindTargets = bindTypesResolver.findBindToKTypes(annotationSource, AUTOBINDS_NAME)
        if (bindTargets != null &&
                !bindTypesResolver.validateBindTargets(annotatedClass, bindTargets, originAnnotationName)) {
            return
        }

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            hiltComponentClassName = resolved.hiltComponentClassName,
            scopeClassName = resolved.scopeClassName,
            hasScopeAnnotation = resolved.hasScopeAnnotation,
            isObjectModuleRequired = resolved.isObjectModuleRequired,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
            bindTargets = bindTargets,
        )
        val moduleType = getModuleType(annotationSource)
        if (moduleType != null) {
            onGenerateHiltModule.invoke(moduleType, moduleInfo)
        }
    }

    @Suppress("ReturnCount")
    private fun processAutoBindsIntoSet(
        annotatedClass: KSClassDeclaration,
        onGenerateHiltModule: (ModuleType, ModuleInfo) -> Unit,
    ) {
        val annotation = annotatedClass
            .getAnnotationsByType(AutoBindsIntoSet::class)
            .firstOrNull() ?: run {
                logger.error(
                    "Can't find AutoBindsIntoSet annotation for class ${annotatedClass.simpleName}",
                    annotatedClass
                )
                return
            }

        val resolved = componentResolver.resolve(
            declaredComponent = annotation.installIn,
            annotatedClass = annotatedClass,
            annotationSource = annotatedClass,
            annotationName = AUTOBINDS_INTO_SET_NAME,
        ) ?: return

        val bindTargets = bindTypesResolver.findBindToKTypes(annotatedClass, AUTOBINDS_INTO_SET_NAME)
        if (bindTargets != null &&
                !bindTypesResolver.validateBindTargets(annotatedClass, bindTargets, AUTOBINDS_INTO_SET_NAME)) {
            return
        }

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            hiltComponentClassName = resolved.hiltComponentClassName,
            scopeClassName = resolved.scopeClassName,
            annotationSource = annotatedClass,
            moduleNameSuffix = "__IntoSetModule",
            hasScopeAnnotation = resolved.hasScopeAnnotation,
            isObjectModuleRequired = resolved.isObjectModuleRequired,
            annotationName = requireNotNull(AutoBindsIntoSet::class.simpleName),
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
        return findFactoryKType(annotationSource)
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

    private fun findFactoryKType(annotationSource: KSClassDeclaration): KSType? {
        return annotationSource
            .annotations
            .firstOrNull { it.shortName.asString() == AUTOBINDS_NAME }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == FACTORY_ARG_NAME }
            ?.value as? KSType
    }

    /**
     * Returns true if this annotation declaration has [@Target] that includes
     * [AnnotationTarget.CLASS].
     */
    private fun KSClassDeclaration.targetsClass(): Boolean {
        val targetAnnotation = annotations.firstOrNull {
            it.shortName.asString() == "Target"
        } ?: return false

        @Suppress("UNCHECKED_CAST")
        val targetList = targetAnnotation.arguments
            .firstOrNull()
            ?.value as? List<*>
            ?: return false

        return (targetList.singleOrNull() as? KSClassDeclaration)
            ?.qualifiedName?.asString() == "kotlin.annotation.AnnotationTarget.CLASS"
    }

    private companion object {
        const val AUTOBINDS_NAME = "AutoBinds"
        const val AUTOBINDS_INTO_SET_NAME = "AutoBindsIntoSet"
        const val FACTORY_ARG_NAME = "factory"

        const val META_BINDING_INFO = "MetaAutoBindingInfo"
        const val META_ARG_NAME = "qualifiedMetaAnnotationName"
    }
}
