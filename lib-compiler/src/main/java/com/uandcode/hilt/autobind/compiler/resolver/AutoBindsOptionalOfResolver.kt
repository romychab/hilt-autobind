package com.uandcode.hilt.autobind.compiler.resolver

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.AutoBindsOptionalOf
import com.uandcode.hilt.autobind.compiler.Const.AUTOBINDS_OPTIONAL_OF_NAME
import com.uandcode.hilt.autobind.compiler.CustomComponentResolver
import com.uandcode.hilt.autobind.compiler.ModuleType
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.resolver.base.AutoResolver
import kotlin.reflect.KClass

internal class AutoBindsOptionalOfResolver(
    hiltModuleGenerator: HiltModuleGenerator,
    customComponentResolver: CustomComponentResolver,
) : AutoResolver(hiltModuleGenerator, customComponentResolver) {

    override val annotationClass: KClass<out Annotation> = AutoBindsOptionalOf::class

    override fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    ) {
        val annotation = annotationSource.requireAnnotation(AutoBindsOptionalOf::class, annotatedClass)

        val moduleInfo = buildModuleInfo(
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            originAnnotationName = originAnnotationName,
            annotationShortName = AUTOBINDS_OPTIONAL_OF_NAME,
            installInComponent = annotation.installIn,
            moduleNameSuffix = "__OptionalModule",
        )
        generator.generateHiltModule(ModuleType.OptionalOf, moduleInfo)
    }
}
