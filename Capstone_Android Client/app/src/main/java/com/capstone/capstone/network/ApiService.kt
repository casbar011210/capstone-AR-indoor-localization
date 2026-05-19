package com.capstone.capstone.network

import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET("path")
    fun getPath(): Call<JsonObject>

    @Multipart
    @POST("rebuild")
    fun uploadMappingImage(@Part file: MultipartBody.Part): Call<JsonObject>

    @FormUrlEncoded
    @POST("rebuild")
    fun finishRebuild(@Field("done") done: String = "true"): Call<JsonObject>

    @Multipart
    @POST("localize")
    fun localize(
        @Part file: MultipartBody.Part,
        @Part("meta") meta: RequestBody? = null
    ): Call<JsonObject>
}
