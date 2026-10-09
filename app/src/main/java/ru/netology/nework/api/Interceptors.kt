package ru.netology.nework.api

import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import ru.netology.nework.BuildConfig
import ru.netology.nework.auth.AppAuth

fun loggingInterceptors() = HttpLoggingInterceptor().apply {
    if (BuildConfig.DEBUG) {
        level = HttpLoggingInterceptor.Level.BODY
    }
}

fun authInterceptors(auth: AppAuth) = Interceptor { chain ->
    val request = chain.request()
    val builder = request.newBuilder()
        .addHeader("Api-Key", BuildConfig.API_KEY)

    auth.authStateFlow.value.token?.let { token ->
        builder.addHeader("Authorization", token)
    }

    chain.proceed(builder.build())
}