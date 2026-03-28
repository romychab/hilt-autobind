package com.uandcode.hilt.autobind.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.sourcesGeneratedBySymbolProcessor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Helper for compile-testing the KSP processor.
 */
object CompilationTestHelper {

    fun compile(vararg sources: SourceFile): JvmCompilationResult {
        return KotlinCompilation().apply {
            this.sources = sources.toList()
            configureKsp {
                symbolProcessorProviders += AutoBindingSymbolProcessorProvider()
            }
            inheritClassPath = true
        }.compile()
    }

    fun JvmCompilationResult.assertOk() {
        assertEquals(KotlinCompilation.ExitCode.OK, exitCode, "Compilation failed:\n$messages")
    }

    fun JvmCompilationResult.assertCompilationError() {
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, exitCode)
    }

    fun JvmCompilationResult.assertHasGeneratedFile(fileName: String): String {
        val generatedFiles = sourcesGeneratedBySymbolProcessor.toList()
        val generated = generatedFiles
            .firstOrNull { it.name == fileName }
        assertTrue(generated != null, "Expected generated file '$fileName' but found: ${generatedFiles.map { it.name }}")
        return generated!!.readText()
    }

    fun JvmCompilationResult.assertNoGeneratedFile(fileName: String) {
        val generatedFiles = sourcesGeneratedBySymbolProcessor.toList()
        val generated = generatedFiles
            .firstOrNull { it.name == fileName }
        assertTrue(generated == null, "Expected no generated file '$fileName' but it was found")
    }

    fun String.assertContent(expected: String) {
        assertEquals(compacted(), expected.compacted())
    }

    private fun String.compacted(): String {
        return split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
    }
}
