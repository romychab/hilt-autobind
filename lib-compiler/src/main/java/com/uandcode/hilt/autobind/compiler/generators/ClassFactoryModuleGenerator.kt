@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.kspFail
import com.uandcode.hilt.autobind.factories.AutoScoped
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import dagger.Provides
import javax.inject.Inject

/**
 * Generates an object-based Hilt module with a `@Provides` function
 * that delegates instance creation to a [ClassBindingFactory].
 */
internal class ClassFactoryModuleGenerator {

    fun generate(
        moduleInfo: ModuleInfo,
        factoryDeclaration: KSClassDeclaration,
    ): TypeSpec = with(moduleInfo) {

        val hasInjectAnnotation = factoryDeclaration
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            kspFail(
                "Factory class must have a primary constructor with @Inject annotation",
                factoryDeclaration,
            )
        }

        // Check if create() is annotated with @AutoScoped
        val createMethod = factoryDeclaration.getDeclaredFunctions()
            .firstOrNull { it.simpleName.asString() == CREATE_METHOD &&
                    it.parameters.singleOrNull()?.isKClassType() == true }
        val isAutoScoped = createMethod?.isAnnotationPresent(AutoScoped::class) == true
                || moduleInfo.hasScopeAnnotation

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

    private fun KSValueParameter.isKClassType(): Boolean {
        return type.resolve().declaration.qualifiedName?.asString() ==
                "kotlin.reflect.KClass"
    }

    private companion object {
        const val CREATE_METHOD = "create"
    }
}
