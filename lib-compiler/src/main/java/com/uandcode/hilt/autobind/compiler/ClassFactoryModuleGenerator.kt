@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.uandcode.hilt.autobind.annotations.factories.AutoScoped
import com.uandcode.hilt.autobind.annotations.factories.ClassBindingFactory
import dagger.Provides
import javax.inject.Inject

/**
 * Generates an object-based Hilt module with a `@Provides` function
 * that delegates instance creation to a [ClassBindingFactory].
 */
internal class ClassFactoryModuleGenerator(
    private val logger: KSPLogger,
) {

    fun generate(
        moduleInfo: ModuleInfo,
        factoryDeclaration: KSClassDeclaration,
    ): TypeSpec? = with(moduleInfo) {

        val hasInjectAnnotation = factoryDeclaration
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            logger.error(
                "Factory class must have a primary constructor with @Inject annotation",
                factoryDeclaration,
            )
            return null
        }

        // Check if create() is annotated with @AutoScoped
        val createMethod = factoryDeclaration.getDeclaredFunctions()
            .firstOrNull { it.simpleName.asString() == CREATE_METHOD && it.modifiers.contains(Modifier.OVERRIDE) }
        val isAutoScoped = createMethod
            ?.isAnnotationPresent(AutoScoped::class) == true

        return TypeSpec.objectBuilder(moduleClassName)
            .applyHiltModuleAnnotationsAndModifiers(hiltComponentClassName)
            .addFunction(
                FunSpec.builder("provide$originSimpleName")
                    .addAnnotation(Provides::class)
                    .apply {
                        if (isAutoScoped) addAnnotation(scopeClassName)
                    }
                    .addParameter("factory", factoryDeclaration.toClassName())
                    .addCode("return factory.create(%T::class)", originClassName)
                    .returns(originClassName)
                    .build()
            )
            .build()
    }

    private companion object {
        const val CREATE_METHOD = "create"
    }
}
