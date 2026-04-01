package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.AnnotationSpec

/**
 * Represents the kind of Hilt module to generate.
 */
internal sealed class ModuleType {

    /** Generate an interface with `@Binds` functions. */
    data object Default : ModuleType()

    /** Generate an interface with `@Binds` and `@IntoSet` functions. */
    data object IntoSet : ModuleType()

    /** Generate an interface with `@Binds`, `@IntoMap`, and a map key annotation functions. */
    data class IntoMap(
        val mapKeyAnnotationSpec: AnnotationSpec,
    ) : ModuleType()

    /** Generate an object with a `@Provides` function that delegates to a factory. */
    data class ClassFactory(
        val factoryDeclaration: KSClassDeclaration,
    ) : ModuleType()

    /** Generate an object with `@Provides` functions for factory delegates and their sub-methods. */
    data class DelegateFactory(
        val factoryDeclaration: KSClassDeclaration,
    ) : ModuleType()

}
