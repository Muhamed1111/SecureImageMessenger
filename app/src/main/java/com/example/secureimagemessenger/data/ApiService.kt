// ApiInterface.kt
package com.example.secureimagemessenger.data

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

data class MessageOut(val message: String)

interface ApiInterface {

    @Multipart
    @POST("/encrypt_and_embed")
    @Streaming
    suspend fun encryptAndEmbed(
        @Part("message") message: RequestBody,
        @Part("password") password: RequestBody,
        @Part cover: MultipartBody.Part? = null  // ako šalješ sliku nosač
    ): Response<ResponseBody>  // vraća PNG bytes kao stream

    @Multipart
    @POST("/extract_and_decrypt")
    suspend fun extractAndDecrypt(
        @Part image: MultipartBody.Part,        // "image": PNG fajl
        @Part("password") password: RequestBody
    ): MessageOut
}
object ApiService {
    fun create(baseUrl: String): ApiInterface {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)              // npr. "http://10.0.2.2:8000"
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ApiInterface::class.java)
    }
}