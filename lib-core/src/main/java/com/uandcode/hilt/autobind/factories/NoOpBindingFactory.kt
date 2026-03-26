package com.uandcode.hilt.autobind.factories

/**
 * Default no-op factory used when no custom factory is specified in
 * [com.uandcode.hilt.autobind.AutoBinds].
 * Signals the compiler to generate a standard `@Binds` module.
 */
public object NoOpBindingFactory : BindingFactory
