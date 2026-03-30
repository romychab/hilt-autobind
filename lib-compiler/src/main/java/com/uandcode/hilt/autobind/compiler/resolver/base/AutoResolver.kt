package com.uandcode.hilt.autobind.compiler.resolver.base

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.uandcode.hilt.autobind.compiler.generators.HiltModuleGenerator
import kotlin.reflect.KClass

internal abstract class AutoResolver(
    protected val generator: HiltModuleGenerator,
) {

    abstract val annotationClass: KClass<out Annotation>

    abstract fun resolve(
        annotatedClass: KSClassDeclaration,
        annotationSource: KSClassDeclaration,
        originAnnotationName: String,
    )

}
