@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import com.uandcode.hilt.autobind.compiler.generators.ClassFactoryModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.DefaultModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.DelegateFactoryModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.IntoSetModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.MetadataGenerator
import com.uandcode.hilt.autobind.compiler.resolver.AnnotatedSymbolsResolver

class AutoBindingSymbolProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private val componentResolver = HiltComponentResolver(logger)
    private val defaultModuleGenerator = DefaultModuleGenerator(logger)
    private val classFactoryModuleGenerator = ClassFactoryModuleGenerator(logger)
    private val delegateFactoryModuleGenerator = DelegateFactoryModuleGenerator(logger)
    private val intoSetModuleGenerator = IntoSetModuleGenerator(logger)
    private val metadataGenerator = MetadataGenerator(codeGenerator)

    private val annotatedSymbolsResolver = AnnotatedSymbolsResolver(
        logger, componentResolver,
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return annotatedSymbolsResolver.processAnnotatedSymbols(
            resolver = resolver,
            onGenerateHiltModule = ::buildHiltModule,
            onGenerateMetaAutoBinding = metadataGenerator::buildMetadataCarrier,
        )
    }

    private fun buildHiltModule(type: ModuleType, moduleInfo: ModuleInfo) {
        val typeSpec = when (type) {
            is ModuleType.Default -> defaultModuleGenerator.generate(moduleInfo)
            is ModuleType.IntoSet -> intoSetModuleGenerator.generate(moduleInfo)
            is ModuleType.ClassFactory -> classFactoryModuleGenerator.generate(
                moduleInfo = moduleInfo,
                factoryDeclaration = type.factoryDeclaration,
            )
            is ModuleType.DelegateFactory -> delegateFactoryModuleGenerator.generate(
                moduleInfo = moduleInfo,
                factoryDeclaration = type.factoryDeclaration,
            )
        }

        if (typeSpec != null) {
            writeModule(
                hiltModuleTypeSpec = typeSpec,
                moduleInfo = moduleInfo,
            )
        }
    }

    private fun writeModule(
        hiltModuleTypeSpec: TypeSpec?,
        moduleInfo: ModuleInfo,
        annotationSource: KSClassDeclaration = moduleInfo.annotationSource,
    ) {
        if (hiltModuleTypeSpec != null) {
            val fileSpec = FileSpec.builder(moduleInfo.moduleClassName)
                .addType(hiltModuleTypeSpec)
                .build()
            val sources = listOfNotNull(
                moduleInfo.annotatedClass.containingFile,
                annotationSource.containingFile.takeIf { it != moduleInfo.annotatedClass
                    .containingFile },
            )
            fileSpec.writeTo(
                codeGenerator = codeGenerator,
                dependencies = Dependencies(
                    aggregating = false,
                    *sources.toTypedArray(),
                )
            )
        }
    }

}

internal const val METADATA_PACKAGE = "com.uandcode.hilt.autobind.metadata"
