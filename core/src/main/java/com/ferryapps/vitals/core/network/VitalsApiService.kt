package com.ferryapps.vitals.core.network

import retrofit2.http.Body
import retrofit2.http.POST

interface VitalsApiService {

    @POST("snapshots")
    suspend fun postSnapshot(@Body payload: Map<String, @JvmSuppressWildcards Any>)
}
