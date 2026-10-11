package ru.netology.nework.error

import android.database.SQLException
import androidx.media3.datasource.HttpDataSource
import java.io.IOException

sealed class AppError(var code: String) : RuntimeException() {
    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is ApiError -> {
                if (e.code.isBlank() || e.code == "Not Found" || e.code == "Bad Request") {
                    e.code = e.status.toString()
                }
                e
            }

            is AppError -> e

            is SQLException -> DbError()

            is HttpDataSource.HttpDataSourceException,
            is IOException -> NetworkError()

            else -> UnknownError()
        }
    }
}

class ApiError(val status: Int, code: String) : AppError(code)
class NetworkError : AppError("error_network")
class DbError : AppError("error_db")
class UnknownError : AppError("error_unknown")