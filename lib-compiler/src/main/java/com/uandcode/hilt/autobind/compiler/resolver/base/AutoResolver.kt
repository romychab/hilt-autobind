@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.resolver.base

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.uandcode.hilt.autobind.HiltComponent
import com.uandcode.hilt.autobind.compiler.AutoBindException
import com.uandcode.hilt.autobind.compiler.AutoBindingParamsResolver
import com.uandcode.hilt.autobind.compiler.CustomComponentResolver
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import com.uandcode.hilt.autobind.compiler.generators.findCustomComponentFqn
import kotlin.reflect.KClass

internal abstract class AutoResolver(
    protected val generator: HiltModuleGenerator,
    customComponentResolver: CustomComponentResolver,
) {

    abstract val annotationClass: KClass<out Annotation>

    private val componentResolver = AutoBindingParamsResolver(customComponentResolver)

    abstract fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    )

    /**
     * Reads the auto-binding annotation of type [kClass] from this annotation source,
     * failing with a compile-time error when it is absent.
     */
    protected fun <T : Annotation> KSClassDeclaration.requireAnnotation(
        kClass: KClass<T>,
        annotatedClass: KSClassDeclaration,
    ): T = getAnnotationsByType(kClass).firstOrNull()
        ?: throw AutoBindException(
            "Can't find ${kClass.simpleName} annotation for class ${annotatedClass.simpleName}",
            annotatedClass,
        )

    /**
     * Resolves the target Hilt component and qualifier, then assembles the [ModuleInfo]
     * shared by every generated module.
     */
    @Suppress("LongParameterList")
    protected fun buildModuleInfo(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
        annotationShortName: String,
        installInComponent: HiltComponent,
        moduleNameSuffix: String = "Module",
        bindTargets: List<KSType>? = null,
    ): ModuleInfo {
        val customComponentFqn = findCustomComponentFqn(annotationSource, annotationShortName)
        val resolvedComponent = componentResolver.resolve(
            installInComponent = installInComponent,
            installInCustomComponentFqn = customComponentFqn,
            annotatedClass = annotatedClass,
            annotationSource = annotationSource,
            annotationName = originAnnotationName,
        )
        return ModuleInfo(
            annotatedClass = annotatedClass,
            autoBindingParams = resolvedComponent,
            annotationSource = annotationSource,
            moduleNameSuffix = moduleNameSuffix,
            annotationName = originAnnotationName,
            bindTargets = bindTargets,
        )
    }
}
