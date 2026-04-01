package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.ModuleType

internal class HiltModuleGenerator(
    logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) {

    private val defaultModuleGenerator = DefaultModuleGenerator(logger)
    private val classFactoryModuleGenerator = ClassFactoryModuleGenerator(logger)
    private val delegateFactoryModuleGenerator = DelegateFactoryModuleGenerator(logger)
    private val intoSetModuleGenerator = IntoSetModuleGenerator(logger)
    private val intoMapModuleGenerator = IntoMapModuleGenerator(logger)

    fun generateHiltModule(
        type: ModuleType,
        moduleInfo: ModuleInfo
    ) {
        val typeSpec = when (type) {
            is ModuleType.Default -> defaultModuleGenerator.generate(moduleInfo)
            is ModuleType.IntoSet -> intoSetModuleGenerator.generate(moduleInfo)
            is ModuleType.IntoMap -> intoMapModuleGenerator.generate(moduleInfo, type.mapKeyAnnotationSpec)
            is ModuleType.ClassFactory -> classFactoryModuleGenerator.generate(
                moduleInfo = moduleInfo,
                factoryDeclaration = type.factoryDeclaration,
            )
            is ModuleType.DelegateFactory -> delegateFactoryModuleGenerator.generate(
                moduleInfo = moduleInfo,
                factoryDeclaration = type.factoryDeclaration,
            )
        }
        writeModule(
            hiltModuleTypeSpec = typeSpec,
            moduleInfo = moduleInfo,
        )
    }

    private fun writeModule(
        hiltModuleTypeSpec: TypeSpec,
        moduleInfo: ModuleInfo,
        annotationSource: KSClassDeclaration = moduleInfo.annotationSource,
    ) {
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
