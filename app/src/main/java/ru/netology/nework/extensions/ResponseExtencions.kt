package ru.netology.nework.extensions

import retrofit2.Response
import ru.netology.nework.error.AppError

fun <T : Any> Response<T>.getOrThrow(): T {
    if (!this.isSuccessful) throw AppError.ApiError(this.code())

    return this.body() ?: throw AppError.ApiError(this.code())
}

@JvmName("getOrThrowUnit")
fun Response<Unit>.getOrThrow() {
    if (!this.isSuccessful) throw AppError.ApiError(this.code())
}