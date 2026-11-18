package com.rifqi.kasirumkm.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface GoogleSheetsApi {
    // Menggunakan @Url agar endpoint private bisa dipassing dinamis
    @POST
    suspend fun syncData(
        @Url url: String,
        @Body request: SyncRequest
    ): Response<SyncResponse>
}