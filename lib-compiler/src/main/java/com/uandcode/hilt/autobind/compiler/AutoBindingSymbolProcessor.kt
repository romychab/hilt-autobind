@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.MetadataGenerator
import com.uandcode.hilt.autobind.compiler.resolver.AnnotatedSymbolsResolver

class AutoBindingSymbolProcessor(
    private val logger: KSPLogger,
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {

    private val metadataGenerator = MetadataGenerator(codeGenerator)
    private val hiltModuleGenerator = HiltModuleGenerator(logger, codeGenerator)

    private val annotatedSymbolsResolver = AnnotatedSymbolsResolver(
        hiltModuleGenerator = hiltModuleGenerator,
        metadataGenerator = metadataGenerator,
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        return try {
            annotatedSymbolsResolver.processAnnotatedSymbols(resolver)
        } catch (e: AutoBindException) {
            logger.error(e.message ?: "Error!", e.symbol)
            emptyList()
        }
    }

}
