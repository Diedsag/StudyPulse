package com.studypulse.sync.core.network

import com.studypulse.sync.core.network.dto.BundleResponse
import com.studypulse.sync.core.network.dto.DeviceRegisterRequest
import com.studypulse.sync.core.network.dto.DeviceRegisterResponse
import com.studypulse.sync.core.network.dto.HealthResponse
import com.studypulse.sync.core.network.dto.PackageListResponse
import com.studypulse.sync.core.network.dto.SyncBatchRequest
import com.studypulse.sync.core.network.dto.SyncBatchResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("/health")
    suspend fun getHealth(): HealthResponse

    @POST("/api/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): DeviceRegisterResponse

    @GET("/api/packages")
    suspend fun getPackages(
        @Query("device_uid") deviceUid: String
    ): PackageListResponse

    @GET("/api/packages/{externalId}/bundle")
    suspend fun getPackageBundle(
        @Path("externalId") externalId: String,
        @Query("device_uid") deviceUid: String
    ): BundleResponse

    @POST("/api/sync/batch")
    suspend fun syncBatch(
        @Body request: SyncBatchRequest
    ): SyncBatchResponse
}