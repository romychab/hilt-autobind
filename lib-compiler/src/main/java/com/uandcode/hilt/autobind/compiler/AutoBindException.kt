package com.uandcode.hilt.autobind.compiler

import com.google.devtools.ksp.symbol.KSNode

class AutoBindException(
    message: String,
    val symbol: KSNode,
) : Exception(message)
