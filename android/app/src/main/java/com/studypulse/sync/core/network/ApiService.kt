package com.studypulse.sync.core.network

import com.studypulse.sync.core.network.dto.HealthResponse
import retrofit2.http.GET

interface ApiService {

    @GET("/health")
    suspend fun getHealth(): HealthResponse
}