package com.capstone.capstone.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    // ☆ 选一条：模拟器 → 宿主机
    private const val BASE_URL = "http://10.19.154.60:5001/"
    // 真机同局域网访问电脑举例：
    // private const val BASE_URL = "http://192.168.1.23:5001/"
    // 若使用 adb reverse tcp:5001 tcp:5001：
    // private const val BASE_URL = "http://127.0.0.1:5001/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)   // 重建可能很慢，放宽
        .writeTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // 末尾必须 /
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    val apiService: ApiService by lazy { retrofit.create(ApiService::class.java) }
}
