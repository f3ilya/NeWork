package ru.netology.nework.error

import android.database.SQLException
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AccessDeniedException

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

            is FileNotFoundException,
            is AccessDeniedException -> FileAccessError()

            is PlaybackException -> {
                val cause = e.cause
                if (cause != null) {
                    from(cause)
                } else {
                    when (e.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> NetworkError()

                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> FileAccessError()

                        else -> LocalAppError("error_playback")
                    }
                }
            }

            is HttpDataSource.HttpDataSourceException,
            is IOException -> NetworkError()

            else -> UnknownError()
        }
    }
}

class LocalAppError(code: String) : AppError(code)
class ApiError(val status: Int, code: String) : AppError(code)
class NetworkError : AppError("error_network")
class DbError : AppError("error_db")
class UnknownError : AppError("error_unknown")
class FileAccessError : AppError("error_file_access")