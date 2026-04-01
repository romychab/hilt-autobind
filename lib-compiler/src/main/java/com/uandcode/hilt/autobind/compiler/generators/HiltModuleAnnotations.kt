package com.uandcode.hilt.autobind.compiler.generators

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import dagger.Module
import dagger.hilt.InstallIn

/**
 * Applies `@Module`, `@InstallIn` and `internal` modifier to a [TypeSpec.Builder].
 */
internal fun TypeSpec.Builder.preBuildHiltModuleTypeSpec(
    hiltComponentClassName: ClassName,
) = apply {
    addAnnotation(Module::class)
    addAnnotation(
        AnnotationSpec.builder(InstallIn::class)
            .addMember("%T::class", hiltComponentClassName)
            .build()
    )
    addModifiers(KModifier.INTERNAL)
}
