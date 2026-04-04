@file:OptIn(KspExperimental::class)

package com.uandcode.hilt.autobind.compiler.generators

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.uandcode.hilt.autobind.compiler.ModuleInfo
import dagger.Binds

/**
 * Generates an interface-based Hilt module with `@Binds` functions
 * for each directly implemented interface.
 */
internal class DefaultModuleGenerator(
    logger: KSPLogger,
) : AbstractModuleGenerator(logger) {

    fun generate(moduleInfo: ModuleInfo): TypeSpec = with(moduleInfo) {
        val targetSuperTypes = validateCommonBindingRules()
        return TypeSpec.interfaceBuilder(className = moduleClassName)
            .preBuildHiltModuleTypeSpec(hiltComponentClassName)
            .apply {
                addBindFunctions(moduleInfo, originClassName, targetSuperTypes)
            }
            .build()
    }

    private fun TypeSpec.Builder.addBindFunctions(
        moduleInfo: ModuleInfo,
        originClassName: ClassName,
        targetSuperTypes: List<KSType>,
    ) = forEachTargetSuperType(moduleInfo, targetSuperTypes) {
        val funSpec = FunSpec.builder(it.functionName)
            .addParameter(name = "impl", type = originClassName)
            .addAnnotation(Binds::class)
            .apply {
                val scope = moduleInfo.scopeClassName
                if (moduleInfo.isScopedBindingRequired && scope != null) {
                    addAnnotation(scope)
                }
            }
            .applyQualifier(moduleInfo)
            .addModifiers(KModifier.ABSTRACT)
            .returns(it.supertypeTypeName)
            .build()
        addFunction(funSpec)
    }

}
