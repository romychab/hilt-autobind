@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeSpec
import com.uandcode.hilt.autobind.compiler.ModuleInfo

/**
 * Generates an interface-based Hilt module with `@Binds` functions
 * for each directly implemented interface.
 */
internal class DefaultModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(
        moduleInfo: ModuleInfo,
        isObject: Boolean,
    ): TypeSpec = with(moduleInfo) {
        val targetSuperTypes = validateCommonBindingRules()
        return createTypeSpecBuilder(isObject)
            .preBuildHiltModuleTypeSpec(hiltComponentClassName)
            .apply {
                addBindFunctions(moduleInfo, originClassName, targetSuperTypes, isObject)
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindFunctions(
        moduleInfo: ModuleInfo,
        originClassName: ClassName,
        targetSuperTypes: List<KSType>,
        isObject: Boolean,
    ) = forEachTargetSuperType(moduleInfo, targetSuperTypes) { targetSuperType ->
        preBuildBinder(moduleInfo, targetSuperType, originClassName, isObject)
            .build()
            .let(::addFunction)
    }

}
