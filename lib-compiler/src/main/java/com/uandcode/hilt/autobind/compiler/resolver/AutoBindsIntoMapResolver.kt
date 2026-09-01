package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.AutoBindsIntoMap
import com.uandcode.hilt.autobind.compiler.Const.AUTOBINDS_INTO_MAP_NAME
import com.uandcode.hilt.autobind.compiler.CustomComponentResolver
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import com.uandcode.hilt.autobind.compiler.resolver.collectors.BindingTypesCollector
import com.uandcode.hilt.autobind.compiler.resolver.collectors.MapKeyCollector
import kotlin.reflect.KClass

internal class AutoBindsIntoMapResolver(
    hiltModuleGenerator: HiltModuleGenerator,
    customComponentResolver: CustomComponentResolver,
) : AutoResolver(hiltModuleGenerator, customComponentResolver) {

    override val annotationClass: KClass<out Annotation> = AutoBindsIntoMap::class

    private val bindingTypesCollector = BindingTypesCollector()
    private val mapKeyCollector = MapKeyCollector()

    override fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    ) {
        val annotation = annotationSource.requireAnnotation(AutoBindsIntoMap::class, annotatedClass)

        val bindTargets = bindingTypesCollector.findBindToKTypes(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationShortName = AUTOBINDS_INTO_MAP_NAME,
            originAnnotationName = originAnnotationName,
        )

        val mapKeyAnnotationSpec = mapKeyCollector.collect(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            originAnnotationName = originAnnotationName,
        )

        val moduleInfo = buildModuleInfo(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            originAnnotationName = originAnnotationName,
            annotationShortName = AUTOBINDS_INTO_MAP_NAME,
            installInComponent = annotation.installIn,
            moduleNameSuffix = "__IntoMapModule",
            bindTargets = bindTargets,
        )
        val isObject = annotatedClass.classKind == ClassKind.OBJECT
        generator.generateHiltModule(ModuleType.IntoMap(mapKeyAnnotationSpec, isObject), moduleInfo)
    }

}
