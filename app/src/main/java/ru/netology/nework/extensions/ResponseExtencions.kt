package ru.netology.nework.extensions

import retrofit2.Response
import ru.netology.nework.error.ApiError

fun <T> Response<T>.getOrThrow(): T {
    if (!this.isSuccessful) throw ApiError(this.code(), this.message())

    return this.body() ?: throw ApiError(this.code(), "Response body is null!")
}

fun Response<Unit>.getOrThrow() {
    if (!this.isSuccessful) throw ApiError(this.code(), this.message())
}