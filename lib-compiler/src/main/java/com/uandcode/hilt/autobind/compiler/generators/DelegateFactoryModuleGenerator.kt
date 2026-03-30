@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isConstructor
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
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.kspFail
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
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(
        moduleInfo: ModuleInfo,
        factoryDeclaration: KSClassDeclaration,
    ): TypeSpec = with(moduleInfo) {

        validateFactory(factoryDeclaration, moduleInfo.annotatedClass)

        val factoryClassName = factoryDeclaration.toClassName()

        // Check if provideDelegate() is annotated with @AutoScoped
        val provideDelegateMethod = factoryDeclaration.getAllFunctions()
            .firstOrNull {
                it.simpleName.asString() == PROVIDE_DELEGATE_METHOD && it.modifiers.contains(Modifier.OVERRIDE)
            }
        val isAutoScoped = provideDelegateMethod
            ?.isAnnotationPresent(AutoScoped::class) == true ||
                moduleInfo.hasScopeAnnotation

        val delegateMethods = annotatedClass.getDeclaredFunctions()
            .filter { !it.isConstructor() }
            .filter { it.isPublic() }
            .filter { it.parameters.isEmpty() }
            .filter { isNonUnitReturn(it) }
            .toList()

        for (method in delegateMethods) {
            if (method.isAnnotationPresent(AutoScoped::class)) {
                logger.warn(
                    "@AutoScoped on '${method.simpleName.asString()}()' is ignored. " +
                        "Place @AutoScoped on ${factoryDeclaration.simpleName.asString()}.provideDelegate() " +
                        "to scope the delegate instance.",
                    method,
                )
            }
        }

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

    private fun ModuleInfo.validateFactory(
        factoryDeclaration: KSClassDeclaration,
        annotatedClass: KSClassDeclaration,
    ) {
        val factoryName = factoryDeclaration.simpleName.asString()

        if (factoryDeclaration.typeParameters.isNotEmpty()) {
            logFactoryTypeParamsError(factoryName, factoryDeclaration)
        }

        if (annotatedClass.typeParameters.isNotEmpty()) {
            commonKspFail("should not have type parameters", annotatedClass)
        }

        if (factoryDeclaration.classKind != ClassKind.CLASS) {
            kspFail(
                "DelegateBindingFactory subclass '$factoryName' must be a class (not an object, interface, or enum)",
                factoryDeclaration
            )
        }

        if (factoryDeclaration.isAbstract()) {
            kspFail(
                "DelegateBindingFactory subclass '$factoryName' must not be abstract",
                factoryDeclaration,
            )
        }

        if (Modifier.OPEN in factoryDeclaration.modifiers) {
            kspFail(
                "DelegateBindingFactory subclass '$factoryName' must be a final class",
                factoryDeclaration,
            )
        }

        val hasInjectAnnotation = factoryDeclaration
            .primaryConstructor
            ?.isAnnotationPresent(Inject::class)
        if (hasInjectAnnotation != true) {
            logNoInjectAnnotationError(factoryName, factoryDeclaration)
        }

        val provideDelegateFun = findProvideDelegateFunction(factoryDeclaration)
            ?: kspFail(
            "provideDelegate() function has not been found. " +
                    "Make sure you override it.", factoryDeclaration
            )

        if (provideDelegateFun.returnType?.resolve() != annotatedClass.asStarProjectedType()) {
            kspFail("The provideDelegate() function must return '${annotatedClass.qualifiedName?.asString()}' " +
                    "type, since the factory is used in @AutoBinds annotation for that type.", factoryDeclaration)
        }

        if (findDelegateBindingFactoryType(factoryDeclaration) == null) {
            kspFail(
                "DelegateBindingFactory subclass '$factoryName' must directly implement DelegateBindingFactory",
                factoryDeclaration,
            )
        }
    }

    private fun findProvideDelegateFunction(
        factoryDeclaration: KSClassDeclaration,
    ) = factoryDeclaration.getDeclaredFunctions()
        .firstOrNull {
            it.simpleName.asString() == "provideDelegate" &&
                    it.parameters.isEmpty() &&
                    it.modifiers.contains(Modifier.OVERRIDE)
        }

    private fun logFactoryTypeParamsError(
        factoryName: String,
        factoryDeclaration: KSClassDeclaration,
    ): Nothing {
        kspFail(
            "DelegateBindingFactory subclass '$factoryName' must not have type parameters. " +
                    "Specify the concrete type on the parent interface instead " +
                    "(e.g., DelegateBindingFactory<AppDatabase>)",
            factoryDeclaration,
        )
    }

    private fun logNoInjectAnnotationError(factoryName: String, factoryDeclaration: KSClassDeclaration): Nothing {
        kspFail(
            "DelegateBindingFactory subclass '$factoryName' must have a primary constructor " +
                    "with @Inject annotation",
            factoryDeclaration,
        )
    }

    private fun isNonUnitReturn(function: KSFunctionDeclaration): Boolean {
        val returnType = function.returnType?.resolve() ?: return false
        return returnType.declaration.qualifiedName?.asString() != "kotlin.Unit"
    }

    private fun findDelegateBindingFactoryType(
        factoryDeclaration: KSClassDeclaration,
    ) = factoryDeclaration.superTypes
        .map { it.resolve() }
        .firstOrNull {
            it.declaration.qualifiedName?.asString() == DelegateBindingFactory::class.qualifiedName
        }

    private companion object {
        const val PROVIDE_DELEGATE_METHOD = "provideDelegate"
    }
}
