package com.uandcode.hilt.autobind.app.debug

import com.uandcode.hilt.autobind.AutoBindsOptionalOf

/**
 * Demonstrates optional bindings: no implementation of this interface is provided in
 * this app, so injecting `Optional<DebugPanel>` yields an empty `Optional`. Adding an
 * `@AutoBinds` implementation in any module (for a debug flavor, for instance) makes
 * it present without changing the consumer.
 */
@AutoBindsOptionalOf
interface DebugPanel {
    fun show()
}
