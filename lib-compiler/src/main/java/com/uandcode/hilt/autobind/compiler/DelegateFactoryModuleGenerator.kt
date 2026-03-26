@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isPublic
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.uandcode.hilt.autobind.factories.AutoScoped
import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
import dagger.Provides
import javax.inject.Inject

/**
 * Generates an object-based Hilt module for [DelegateBindingFactory].
 *
 * Calls [DelegateBindingFactory.provideDelegate] to obtain the delegate instance,
 * then generates `@Provides` functions for each public non-Unit-returning method
 * of the annotated type.
 */
internal class DelegateFactoryModuleGenerator(
    private val logger: KSPLogger,
) {

    fun generate(
        moduleInfo: ModuleInfo,
        factoryDeclaration: KSClassDeclaration,
    ): TypeSpec? = with(moduleInfo) {

        if (!validateFactory(factoryDeclaration)) return null

        val factoryClassName = factoryDeclaration.toClassName()

        // Check if provideDelegate() is annotated with @AutoScoped
        val provideDelegateMethod = factoryDeclaration.getAllFunctions()
            .firstOrNull { it.simpleName.asString() == PROVIDE_DELEGATE_METHOD && it.modifiers.contains(Modifier.OVERRIDE) }
        val isAutoScoped = provideDelegateMethod
            ?.isAnnotationPresent(AutoScoped::class) == true

        // Find delegate methods: public non-Unit-returning methods declared directly on the annotated class
        val delegateMethods = annotatedClass.getDeclaredFunctions()
            .filter { !it.simpleName.asString().contains("<") }
            .filter { it.isPublic() }
            .filter { isNonUnitReturn(it) }
            .toList()

        val builder = TypeSpec.objectBuilder(moduleClassName)
            .applyHiltModuleAnnotationsAndModifiers(hiltComponentClassName)

        // @Provides for the main delegate via provideDelegate()
        builder.addFunction(
            FunSpec.builder("provide$originSimpleName")
                .addAnnotation(Provides::class)
                .apply {
                    if (isAutoScoped) addAnnotation(scopeClassName)
                }
                .addParameter("factory", factoryClassName)
                .addCode("return factory.provideDelegate()")
                .returns(originClassName)
                .build()
        )

        // @Provides for each sub-delegate method of the annotated type
        for (delegateMethod in delegateMethods) {
            val returnType = delegateMethod.returnType?.resolve() ?: continue
            val returnTypeName = returnType.toTypeName()
            val methodName = delegateMethod.simpleName.asString()
            val provideMethodName = "provide${methodName.replaceFirstChar { it.uppercase() }}"

            builder.addFunction(
                FunSpec.builder(provideMethodName)
                    .addAnnotation(Provides::class)
                    .addParameter("delegate", originClassName)
                    .addCode("return delegate.%N()", methodName)
                    .returns(returnTypeName)
                    .build()
            )
        }

        return builder.build()
    }

    private fun validateFactory(factoryDeclaration: KSClassDeclaration): Boolean {
        val factoryName = factoryDeclaration.simpleName.asString()

        if (factoryDeclaration.typeParameters.isNotEmpty()) {
            logger.error(
                "DelegateBindingFactory subclass '$factoryName' must not have type parameters. " +
                    "Specify the concrete type on the parent interface instead " +
                    "(e.g., DelegateBindingFactory<AppDatabase>)",
                factoryDeclaration,
            )
            return false
        }

        if (factoryDeclaration.classKind != ClassKind.CLASS) {
            logger.error(
                "DelegateBindingFactory subclass '$factoryName' must be a class " +
                    "(not an object, interface, or enum)",
                factoryDeclaration,
            )
            return false
        }

        if (factoryDeclaration.isAbstract()) {
            logger.error(
                "DelegateBindingFactory subclass '$factoryName' must not be abstract",
                factoryDeclaration,
            )
            return false
        }

        if (Modifier.OPEN in factoryDeclaration.modifiers) {
            logger.error(
                "DelegateBindingFactory subclass '$factoryName' must be a final class",
                factoryDeclaration,
            )
            return false
        }

        val hasInjectAnnotation = factoryDeclaration
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            logger.error(
                "DelegateBindingFactory subclass '$factoryName' must have a primary constructor " +
                    "with @Inject annotation",
                factoryDeclaration,
            )
            return false
        }

        // Verify the type argument of DelegateBindingFactory resolves to a concrete type
        val delegateBindingFactoryType = factoryDeclaration.superTypes
            .map { it.resolve() }
            .firstOrNull {
                it.declaration.qualifiedName?.asString() == DelegateBindingFactory::class.qualifiedName
            }
        if (delegateBindingFactoryType == null) {
            logger.error(
                "DelegateBindingFactory subclass '$factoryName' must directly implement DelegateBindingFactory",
                factoryDeclaration,
            )
            return false
        }

        return true
    }

    private fun isNonUnitReturn(function: KSFunctionDeclaration): Boolean {
        val returnType = function.returnType?.resolve() ?: return false
        return returnType.declaration.qualifiedName?.asString() != "kotlin.Unit"
    }

    private companion object {
        const val PROVIDE_DELEGATE_METHOD = "provideDelegate"
    }
}
