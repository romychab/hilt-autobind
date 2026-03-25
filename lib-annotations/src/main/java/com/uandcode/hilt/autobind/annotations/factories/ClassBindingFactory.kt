package com.uandcode.hilt.autobind.annotations.factories

import kotlin.reflect.KClass

/**
 * A factory that creates instances of annotated classes by their [KClass].
 *
 * Implement this interface and annotate the implementation with `@Inject` to
 * use it with `@AutoBinds(factory = ...)`. The generated Hilt module will call
 * [create] to provide the annotated type.
 *
 * Example: Retrofit API factory:
 * ```
 * class RetrofitBindingFactory @Inject constructor(
 *     private val retrofit: Retrofit,
 * ) : ClassBindingFactory {
 *     override fun <T : Any> create(kClass: KClass<T>): T =
 *         retrofit.create(kClass.java)
 * }
 * ```
 */
public interface ClassBindingFactory : BindingFactory {

    /**
     * Creates an instance of [T] identified by [kClass].
     *
     * Annotate with [AutoScoped] to scope the generated `@Provides` function.
     */
    public fun <T : Any> create(kClass: KClass<T>): T

}
