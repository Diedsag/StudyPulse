package com.studypulse.sync.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val time: String
)