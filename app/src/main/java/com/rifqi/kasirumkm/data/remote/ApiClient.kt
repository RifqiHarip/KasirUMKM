package com.rifqi.kasirumkm.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    // 1. Buat CCTV untuk memantau request dan response
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Level.BODY akan mencetak URL LENGKAP, Header, dan isi JSON-nya ke Logcat
        level = HttpLoggingInterceptor.Level.BODY
    }

    // 2. Pasang CCTV ke OkHttpClient
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    // 3. Masukkan client ke Retrofit
    val api: GoogleSheetsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://script.google.com/")
            .client(client) // <-- Tambahkan baris ini
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleSheetsApi::class.java)
    }
}