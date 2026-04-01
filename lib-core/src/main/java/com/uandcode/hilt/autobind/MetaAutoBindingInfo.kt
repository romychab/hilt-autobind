package com.uandcode.hilt.autobind

/**
 * For internal usage. This annotation is used by the KSP
 * annotation processor to make the library work in multi-module
 * environments.
 *
 * @property qualifiedMetaAnnotationName The fully-qualified name of auto-binding
 *   annotation alias
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
public annotation class MetaAutoBindingInfo(
    val qualifiedMetaAnnotationName: String,
)
