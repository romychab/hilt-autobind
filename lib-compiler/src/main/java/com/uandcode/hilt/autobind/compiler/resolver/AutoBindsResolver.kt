@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.compiler.AutoBindException
import com.uandcode.hilt.autobind.compiler.AutoBindingParamsResolver
import com.uandcode.hilt.autobind.compiler.Const.AUTOBINDS_NAME
import com.uandcode.hilt.autobind.compiler.CustomComponentResolver
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.findCustomComponentFqn
import com.uandcode.hilt.autobind.compiler.generators.findFactoryKType
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import com.uandcode.hilt.autobind.compiler.resolver.collectors.BindingTypesCollector
import com.uandcode.hilt.autobind.factories.ClassBindingFactory
import com.uandcode.hilt.autobind.factories.DelegateBindingFactory
import com.uandcode.hilt.autobind.factories.NoOpBindingFactory
import kotlin.reflect.KClass

internal class AutoBindsResolver(
    hiltModuleGenerator: HiltModuleGenerator,
    customComponentResolver: CustomComponentResolver,
) : AutoResolver(hiltModuleGenerator) {

    override val annotationClass: KClass<out Annotation> = AutoBinds::class

    private val componentResolver = AutoBindingParamsResolver(customComponentResolver)
    private val bindingTypesCollector = BindingTypesCollector()

    override fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    ) {
        val annotation = annotationSource
            .getAnnotationsByType(AutoBinds::class)
            .firstOrNull()
        if (annotation == null) {
            throw AutoBindException(
                "Can't find AutoBinds annotation for class ${annotatedClass.simpleName}",
                annotatedClass,
            )
        }

        val customComponentFqn = findCustomComponentFqn(annotationSource, AUTOBINDS_NAME)

        val resolvedComponent = componentResolver.resolve(
            installInComponent = annotation.installIn,
            installInCustomComponentFqn = customComponentFqn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
        )

        val bindTargets = bindingTypesCollector.findBindToKTypes(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationShortName = AUTOBINDS_NAME,
            originAnnotationName = originAnnotationName,
        )

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            autoBindingParams = resolvedComponent,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
            bindTargets = bindTargets,
        )
        val isObject = annotatedClass.classKind == ClassKind.OBJECT
        val moduleType = getModuleType(annotationSource, isObject)
        generator.generateHiltModule(moduleType, moduleInfo)
    }

    private fun getModuleType(
        annotationSource: KSClassDeclaration,
        isObject: Boolean,
    ): ModuleType {
        return findFactoryKType(annotationSource, AUTOBINDS_NAME)
            ?.let { it.declaration as? KSClassDeclaration }
            ?.takeIf { it.qualifiedName?.asString() != NoOpBindingFactory::class.qualifiedName }
            ?.let { factoryDeclaration ->
                val superTypeNames = factoryDeclaration.superTypes
                    .map { it.resolve().declaration.qualifiedName?.asString() }
                    .toList()
                if (ClassBindingFactory::class.qualifiedName in superTypeNames) {
                    ModuleType.ClassFactory(factoryDeclaration)
                } else if (DelegateBindingFactory::class.qualifiedName in superTypeNames) {
                    ModuleType.DelegateFactory(factoryDeclaration)
                } else {
                    throw AutoBindException(
                        "AutoBinds Factory class '${factoryDeclaration.simpleName.asString()}' " +
                                "must directly implement ClassBindingFactory or DelegateBindingFactory",
                        factoryDeclaration,
                    )
                }
            } ?: ModuleType.Default(isObject)
    }
}
