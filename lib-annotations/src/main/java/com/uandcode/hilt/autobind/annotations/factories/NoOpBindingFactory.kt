package com.uandcode.hilt.autobind.annotations.factories

/**
 * Default no-op factory used when no custom factory is specified in
 * [com.uandcode.hilt.autobind.annotations.AutoBinds].
 * Signals the compiler to generate a standard `@Binds` module.
 */
public object NoOpBindingFactory : BindingFactory
