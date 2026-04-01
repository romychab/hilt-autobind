@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.AutoBindsIntoMap
import com.uandcode.hilt.autobind.compiler.AutoBindException
import com.uandcode.hilt.autobind.compiler.AutoBindingParamsResolver
import com.uandcode.hilt.autobind.compiler.Const.AUTOBINDS_INTO_MAP_NAME
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import com.uandcode.hilt.autobind.compiler.resolver.collectors.BindingTypesCollector
import com.uandcode.hilt.autobind.compiler.resolver.collectors.MapKeyCollector
import kotlin.reflect.KClass

internal class AutoBindsIntoMapResolver(
    hiltModuleGenerator: HiltModuleGenerator,
) : AutoResolver(hiltModuleGenerator) {

    override val annotationClass: KClass<out Annotation> = AutoBindsIntoMap::class

    private val componentResolver = AutoBindingParamsResolver()
    private val bindingTypesCollector = BindingTypesCollector()
    private val mapKeyCollector = MapKeyCollector()

    override fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    ) {
        val annotation = annotationSource
            .getAnnotationsByType(AutoBindsIntoMap::class)
            .firstOrNull() ?: throw AutoBindException(
                "Can't find AutoBindsIntoMap annotation for class ${annotatedClass.simpleName}",
                annotatedClass,
            )

        val resolvedComponent = componentResolver.resolve(
            installInComponent = annotation.installIn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
        )

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

        val moduleInfo = ModuleInfo(
            annotatedClass = annotatedClass,
            autoBindingParams = resolvedComponent,
            annotationSource = annotationSource,
            moduleNameSuffix = "__IntoMapModule",
            annotationName = originAnnotationName,
            bindTargets = bindTargets,
        )
        generator.generateHiltModule(ModuleType.IntoMap(mapKeyAnnotationSpec), moduleInfo)
    }

}
