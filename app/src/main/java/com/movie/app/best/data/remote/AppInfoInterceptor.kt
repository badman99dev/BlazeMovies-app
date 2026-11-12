package com.movie.app.best.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.movie.app.best.BuildConfig
import com.movie.app.best.data.update.DeviceAbi
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Tags every request with the client build and device identity, so the backend can
 * tell which app version and ABI is calling.
 *
 * Applied only to our own backends (main API, imdx, sports, zee5, download bypass) —
 * not to third-party media origins, which are strict about extra headers.
 */
object AppInfoInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-App-Version-Name", BuildConfig.VERSION_NAME)
            .header("X-App-Version-Code", BuildConfig.VERSION_CODE.toString())
            .header("X-App-Abi", DeviceAbi.primary)
            .header("X-App-Abi-List", DeviceAbi.list)
            .header("X-App-Abi-Bits", DeviceAbi.bits.toString())
            .header("X-App-Sdk", DeviceAbi.sdk.toString())
            .header("X-App-Device", DeviceAbi.device)
            // 0 = signed in, 1 = guest. Explicit so the backend never has to guess from
            // the (sometimes cached) auth token.
            .header("X-App-Anon", if (FirebaseAuth.getInstance().currentUser == null) "1" else "0")
            .build()
        return chain.proceed(request)
    }
}