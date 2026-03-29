package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import com.uandcode.hilt.autobind.MetaAutoBindingInfo
import com.uandcode.hilt.autobind.compiler.METADATA_PACKAGE
import com.uandcode.hilt.autobind.compiler.MetadataInfo

internal class MetadataGenerator(
    private val codeGenerator: CodeGenerator,
) {

    fun buildMetadataCarrier(metadataInfo: MetadataInfo) {
        val carrierSimpleName = metadataInfo.qualifiedName
            .replace(".", "__")
        val carrierClassName = ClassName(METADATA_PACKAGE, "__$carrierSimpleName")
        val typeSpec = TypeSpec.classBuilder(carrierClassName)
            .addModifiers(KModifier.INTERNAL)
            .addAnnotation(AnnotationSpec.builder(MetaAutoBindingInfo::class)
                .addMember("qualifiedMetaAnnotationName = %S", metadataInfo.qualifiedName)
                .build())
            .build()
        val fileSpec = FileSpec.builder(carrierClassName)
            .addType(typeSpec)
            .build()
        val sources = listOfNotNull(metadataInfo.annotatedAnnotation.containingFile)
        fileSpec.writeTo(
            codeGenerator = codeGenerator,
            dependencies = Dependencies(
                aggregating = false,
                *sources.toTypedArray(),
            )
        )
    }

}
