package com.uandcode.hilt.autobind.app.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RandomImageResponse(
    @SerialName("message") val imageUrl: String,
    val status: String,
)
