package com.example.datacollector.api

import com.example.datacollector.model.DeviceData
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("receive_data.php")
    suspend fun sendData(@Body data: DeviceData): Response<Void>
}

object RetrofitClient {
    // 10.0.2.2 is the special alias to your host loopback interface (i.e., localhost on your development machine)
    private const val BASE_URL = "http://10.0.2.2/cmovil2/api/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(ApiService::class.java)
    }
}
