@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.uandcode.hilt.autobind.compiler.AutoBindException
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.factories.AutoScoped
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import dagger.Provides
import javax.inject.Inject

/**
 * Generates an object-based Hilt module with a `@Provides` function
 * that delegates instance creation to a [ClassBindingFactory].
 */
internal class ClassFactoryModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(
        moduleInfo: ModuleInfo,
        factoryDeclaration: KSClassDeclaration,
    ): TypeSpec = with(moduleInfo) {

        val hasInjectAnnotation = factoryDeclaration
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            throw AutoBindException(
                "Factory class must have a primary constructor with @Inject annotation",
                factoryDeclaration,
            )
        }

        // Check if create() is annotated with @AutoScoped
        val createMethod = factoryDeclaration.getDeclaredFunctions()
            .firstOrNull { it.simpleName.asString() == CREATE_METHOD &&
                    it.parameters.singleOrNull()?.isKClassType() == true }
        val isAutoScoped = createMethod?.isAnnotationPresent(AutoScoped::class) == true
                || moduleInfo.isScopedBindingRequired

        return TypeSpec.objectBuilder(moduleClassName)
            .preBuildHiltModuleTypeSpec(hiltComponentClassName)
            .addFunction(
                FunSpec.builder("provide$originSimpleName")
                    .addAnnotation(Provides::class)
                    .apply {
                        val scope = scopeClassName
                        if (isAutoScoped && scope != null) {
                            addAnnotation(scope)
                        }
                    }
                    .applyQualifier(moduleInfo)
                    .addParameter("factory", factoryDeclaration.toClassName())
                    .addCode("return factory.create(%T::class)", originClassName)
                    .returns(originClassName)
                    .build()
            )
            .build()
    }

    private fun KSValueParameter.isKClassType(): Boolean {
        return type.resolve().declaration.qualifiedName?.asString() ==
                "kotlin.reflect.KClass"
    }

    private companion object {
        const val CREATE_METHOD = "create"
    }
}
