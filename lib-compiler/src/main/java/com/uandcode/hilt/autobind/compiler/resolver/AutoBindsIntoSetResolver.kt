package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.AutoBindsIntoSet
import com.uandcode.hilt.autobind.compiler.Const.AUTOBINDS_INTO_SET_NAME
import com.uandcode.hilt.autobind.compiler.CustomComponentResolver
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import com.uandcode.hilt.autobind.compiler.resolver.collectors.BindingTypesCollector
import kotlin.reflect.KClass

internal class AutoBindsIntoSetResolver(
    hiltModuleGenerator: HiltModuleGenerator,
    customComponentResolver: CustomComponentResolver,
) : AutoResolver(hiltModuleGenerator, customComponentResolver) {

    override val annotationClass: KClass<out Annotation> = AutoBindsIntoSet::class

    private val bindingTypesCollector = BindingTypesCollector()

    override fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String
    ) {
        val annotation = annotationSource.requireAnnotation(AutoBindsIntoSet::class, annotatedClass)

        val bindTargets = bindingTypesCollector.findBindToKTypes(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationShortName = AUTOBINDS_INTO_SET_NAME,
            originAnnotationName = originAnnotationName,
        )

        val moduleInfo = buildModuleInfo(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            originAnnotationName = originAnnotationName,
            annotationShortName = AUTOBINDS_INTO_SET_NAME,
            installInComponent = annotation.installIn,
            moduleNameSuffix = "__IntoSetModule",
            bindTargets = bindTargets,
        )
        val isObject = annotatedClass.classKind == ClassKind.OBJECT
        generator.generateHiltModule(ModuleType.IntoSet(isObject), moduleInfo)
    }
}
