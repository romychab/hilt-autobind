package com.uandcode.hilt.autobind.app.greeter

import com.uandcode.hilt.autobind.AutoBinds
import javax.inject.Inject

// No need to write a Hilt module manually.
// The @AutoBinds annotation auto-generates a Hilt module
// that binds GreeterImpl to the Greeter interface.
@AutoBinds
class GreeterImpl @Inject constructor() : Greeter {

    override fun greet(name: String): String = "Hello, $name!"

}
