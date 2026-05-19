package com.capstone.capstone.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val PREFS_NAME = "server_settings"
    private const val KEY_BASE_URL = "base_url"
    private const val DEFAULT_BASE_URL = "http://127.0.0.1:5001/"

    private lateinit var appContext: Context
    private var cachedBaseUrl: String? = null
    private var cachedRetrofit: Retrofit? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun hasSavedBaseUrl(context: Context = appContext): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .contains(KEY_BASE_URL)
    }

    fun getBaseUrl(context: Context = appContext): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_BASE_URL)
            ?: DEFAULT_BASE_URL
    }

    fun updateBaseUrl(context: Context, rawInput: String): String {
        val normalizedUrl = normalizeBaseUrl(rawInput)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .apply()

        cachedBaseUrl = null
        cachedRetrofit = null
        return normalizedUrl
    }

    private fun normalizeBaseUrl(rawInput: String): String {
        var url = rawInput.trim()
        require(url.isNotEmpty()) { "Server URL cannot be empty" }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }

        if (!url.endsWith("/")) {
            url += "/"
        }

        return url
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor(logging)
        .build()

    private fun getRetrofit(): Retrofit {
        val baseUrl = getBaseUrl()
        val existing = cachedRetrofit
        if (existing != null && cachedBaseUrl == baseUrl) {
            return existing
        }

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .also {
                cachedBaseUrl = baseUrl
                cachedRetrofit = it
            }
    }

    val apiService: ApiService
        get() = getRetrofit().create(ApiService::class.java)
}
