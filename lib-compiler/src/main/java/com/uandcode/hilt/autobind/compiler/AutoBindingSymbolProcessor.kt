@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ksp.writeTo
import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
import com.uandcode.hilt.autobind.factories.NoOpBindingFactory
import kotlin.reflect.KClass

class AutoBindingSymbolProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private val componentResolver = HiltComponentResolver(logger)
    private val defaultModuleGenerator = DefaultModuleGenerator(logger)
    private val classFactoryModuleGenerator = ClassFactoryModuleGenerator(logger)
    private val delegateFactoryModuleGenerator = DelegateFactoryModuleGenerator(logger)
    private val intoSetModuleGenerator = IntoSetModuleGenerator(logger)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val deferred = mutableListOf<KSAnnotated>()
        deferred += processAnnotation<AutoBinds>(resolver) { processAutoBinds(it) }
        deferred += processAnnotation<AutoBindsIntoSet>(resolver) { processAutoBindsIntoSet(it) }
        return deferred
    }

    private inline fun <reified T : Annotation> processAnnotation(
        resolver: Resolver,
        crossinline handler: (KSClassDeclaration) -> Unit,
    ): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(requireNotNull(T::class.qualifiedName))
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        val deferred = mutableListOf<KSAnnotated>()
        for (symbol in symbols) {
            if (!symbol.validate()) {
                deferred.add(symbol)
                continue
            }
            handler(symbol)
        }
        return deferred
    }

    private fun processAutoBinds(annotatedClass: KSClassDeclaration) {
        val annotation = annotatedClass
            .getAnnotationsByType(AutoBinds::class)
            .firstOrNull()
        if (annotation == null) {
            logger.error("Can't find AutoBinds annotation for class ${annotatedClass.simpleName}", annotatedClass)
            return
        }

        val resolved = componentResolver.resolve(
            declaredComponent = annotation.installIn,
            annotatedClass = annotatedClass,
            annotationName = AUTOBINDS_NAME,
        ) ?: return

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            hiltComponentClassName = resolved.hiltComponentClassName,
            scopeClassName = resolved.scopeClassName,
        )

        val hiltModuleTypeSpec = when (val moduleType = getModuleType(annotatedClass)) {
            is ModuleType.ClassFactory -> classFactoryModuleGenerator.generate(
                moduleInfo = moduleInfo,
                factoryDeclaration = moduleType.factoryDeclaration,
            )
            is ModuleType.DelegateFactory -> delegateFactoryModuleGenerator.generate(
                moduleInfo = moduleInfo,
                annotatedClass = annotatedClass,
                factoryDeclaration = moduleType.factoryDeclaration,
            )
            ModuleType.Default -> defaultModuleGenerator.generate(
                moduleInfo = moduleInfo,
                targetInterfaces = annotatedClass.getTargetInterfaces(),
            )
            null -> null
        }

        writeModule(hiltModuleTypeSpec, moduleInfo, annotatedClass)
    }

    private fun processAutoBindsIntoSet(annotatedClass: KSClassDeclaration) {
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
            annotationName = AUTOBINDS_INTO_SET_NAME,
        ) ?: return

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            hiltComponentClassName = resolved.hiltComponentClassName,
            scopeClassName = resolved.scopeClassName,
            moduleNameSuffix = "__IntoSetModule",
        )

        val hiltModuleTypeSpec = intoSetModuleGenerator.generate(
            moduleInfo = moduleInfo,
            targetInterfaces = annotatedClass.getTargetInterfaces(),
        )

        writeModule(hiltModuleTypeSpec, moduleInfo, annotatedClass)
    }

    private fun writeModule(
        hiltModuleTypeSpec: com.squareup.kotlinpoet.TypeSpec?,
        moduleInfo: ModuleInfo,
        annotatedClass: KSClassDeclaration,
    ) {
        if (hiltModuleTypeSpec != null) {
            val fileSpec = FileSpec.builder(moduleInfo.moduleClassName)
                .addType(hiltModuleTypeSpec)
                .build()
            val sources = listOfNotNull(annotatedClass.containingFile)
            fileSpec.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(
                    aggregating = false,
                    *sources.toTypedArray(),
                )
            )
        }
    }

    /**
     * Determines the module generation strategy by reading the `factory` annotation argument.
     *
     * Note: reading the `factory` argument via raw KSP annotation/arguments API instead of
     * [getAnnotationsByType] because the latter returns a Kotlin [KClass] which cannot be
     * inspected for supertypes. We need the [KSType] to check if the factory implements
     * [ClassBindingFactory].
     */
    private fun getModuleType(annotatedClass: KSClassDeclaration): ModuleType? {
        return findFactoryKType(annotatedClass)
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

    private fun KSClassDeclaration.getTargetInterfaces(): List<KSType> {
        return superTypes
            .map { it.resolve() }
            .filter { (it.declaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE }
            .toList()
    }

    private fun findFactoryKType(annotatedClass: KSClassDeclaration): KSType? {
        return annotatedClass
            .annotations
            .firstOrNull { it.shortName.asString() == AUTOBINDS_NAME }
            ?.arguments
            ?.firstOrNull { it.name?.asString() == FACTORY_ARG_NAME }
            ?.value as? KSType
    }

    private companion object {
        const val AUTOBINDS_NAME = "AutoBinds"
        const val AUTOBINDS_INTO_SET_NAME = "AutoBindsIntoSet"
        const val FACTORY_ARG_NAME = "factory"
    }
}
