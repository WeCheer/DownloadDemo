package com.wyc.download.http

import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url


interface ApiService {

    @Streaming
    @GET
    fun executeDownload(@Header("Range") range: String, @Url url: String): Observable<ResponseBody>
}


