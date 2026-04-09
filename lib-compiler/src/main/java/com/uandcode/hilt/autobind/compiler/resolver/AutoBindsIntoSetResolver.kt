@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.compiler.AutoBindException
import com.uandcode.hilt.autobind.compiler.AutoBindingParamsResolver
import com.uandcode.hilt.autobind.compiler.Const.AUTOBINDS_INTO_SET_NAME
import com.uandcode.hilt.autobind.compiler.CustomComponentResolver
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.findCustomComponentFqn
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import com.uandcode.hilt.autobind.compiler.resolver.collectors.BindingTypesCollector
import kotlin.reflect.KClass

internal class AutoBindsIntoSetResolver(
    hiltModuleGenerator: HiltModuleGenerator,
    customComponentResolver: CustomComponentResolver,
) : AutoResolver(hiltModuleGenerator) {

    override val annotationClass: KClass<out Annotation> = AutoBindsIntoSet::class

    private val componentResolver = AutoBindingParamsResolver(customComponentResolver)
    private val bindingTypesCollector = BindingTypesCollector()

    override fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String
    ) {
        val annotation = annotationSource
            .getAnnotationsByType(AutoBindsIntoSet::class)
            .firstOrNull() ?: throw AutoBindException(
                "Can't find AutoBindsIntoSet annotation for class ${annotatedClass.simpleName}",
                annotatedClass
            )

        val customComponentFqn = findCustomComponentFqn(annotationSource, AUTOBINDS_INTO_SET_NAME)

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
            annotationShortName = AUTOBINDS_INTO_SET_NAME,
            originAnnotationName = originAnnotationName,
        )

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            autoBindingParams = resolvedComponent,
            annotationSource = annotationSource,
            moduleNameSuffix = "__IntoSetModule",
            annotationName = originAnnotationName,
            bindTargets = bindTargets,
        )
        val isObject = annotatedClass.classKind == ClassKind.OBJECT
        generator.generateHiltModule(ModuleType.IntoSet(isObject), moduleInfo)
    }
}
