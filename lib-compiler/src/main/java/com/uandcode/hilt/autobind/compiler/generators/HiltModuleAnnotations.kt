package com.uandcode.hilt.autobind.compiler.generators

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toAnnotationSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.generators.AbstractModuleGenerator.TargetSuperType
import dagger.Binds
import dagger.Module
import dagger.Provides
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


internal fun ModuleInfo.createTypeSpecBuilder(
    isObject: Boolean,
): TypeSpec.Builder {
    return if (isObject) {
        TypeSpec.objectBuilder(className = moduleClassName)
    } else {
        TypeSpec.interfaceBuilder(className = moduleClassName)
    }
}

internal fun preBuildBinder(
    moduleInfo: ModuleInfo,
    targetSupertype: TargetSuperType,
    originClassName: ClassName,
    isObject: Boolean,
): FunSpec.Builder {
    return FunSpec.builder(targetSupertype.functionName)
        .addAnnotation(if (isObject) Provides::class else Binds::class)
        .apply {
            if (!isObject) {
                addParameter(name = "impl", type = originClassName)
                addModifiers(KModifier.ABSTRACT)
            } else {
                addCode("return %T", moduleInfo.annotatedClass.toClassName())
            }
        }
        .applyQualifier(moduleInfo)
        .apply {
            val scope = moduleInfo.scopeClassName
            if (moduleInfo.isScopedBindingRequired && scope != null) {
                addAnnotation(scope)
            }
        }
        .returns(targetSupertype.supertypeTypeName)
}

internal fun FunSpec.Builder.applyQualifier(
    moduleInfo: ModuleInfo,
) = apply {
    if (moduleInfo.qualifier != null) {
        addAnnotation(moduleInfo.qualifier.toAnnotationSpec())
    }
}
