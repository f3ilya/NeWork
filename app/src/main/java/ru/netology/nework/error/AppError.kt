package ru.netology.nework.error

import android.database.SQLException
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import retrofit2.HttpException
import ru.netology.nework.R
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.AccessDeniedException

@Suppress("CanSealedSubClassBeObject")
sealed class AppError : RuntimeException() {
    class ApiError(val code: Int) : AppError()
    class NetworkError : AppError()
    class DbError : AppError()
    class FileAccessError : AppError()
    class FileTooLargeError : AppError()
    class PlaybackError : AppError()
    class EmptyContentOrAttachmentError : AppError()
    class EmptyEventDateError : AppError()
    class UnknownError : AppError()

    companion object {
        fun from(e: Throwable): AppError = when (e) {
            is AppError -> e
            is HttpException -> ApiError(e.code())
            is SQLException -> DbError()
            is FileNotFoundException, is AccessDeniedException -> FileAccessError()
            is PlaybackException -> {
                val cause = e.cause
                if (cause != null && cause !is PlaybackException) {
                    from(cause)
                } else {
                    when (e.errorCode) {
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> NetworkError()

                        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> FileAccessError()

                        else -> PlaybackError()
                    }
                }
            }

            is HttpDataSource.HttpDataSourceException,
            is IOException -> NetworkError()

            else -> UnknownError()
        }
    }
}

fun Throwable.toErrorType(
    apiErrorMapper: (code: Int) -> ErrorType = { ErrorType.UNKNOWN }
): ErrorType {
    return when (val appError = AppError.from(this)) {
        is AppError.ApiError -> apiErrorMapper(appError.code)
        is AppError.NetworkError -> ErrorType.NETWORK
        is AppError.DbError -> ErrorType.DB
        is AppError.FileAccessError -> ErrorType.FILE_ACCESS
        is AppError.FileTooLargeError -> ErrorType.FILE_TOO_LARGE
        is AppError.PlaybackError -> ErrorType.PLAYBACK
        is AppError.EmptyContentOrAttachmentError -> ErrorType.EMPTY_CONTENT_OR_ATTACHMENT
        is AppError.EmptyEventDateError -> ErrorType.EMPTY_EVENT_DATE
        is AppError.UnknownError -> ErrorType.UNKNOWN
    }
}

enum class ErrorType(@param:StringRes val messageRes: Int) {
    NETWORK(R.string.error_network),
    DB(R.string.error_db),
    PLAYBACK(R.string.error_playback),
    FILE_ACCESS(R.string.error_file_access),
    FILE_TOO_LARGE(R.string.error_file_too_large),
    UNKNOWN(R.string.error_unknown),
    POST_NOT_FOUND(R.string.error_post_not_found),
    EVENT_NOT_FOUND(R.string.error_event_not_found),
    USER_NOT_FOUND(R.string.error_user_not_found),
    USER_NOT_REGISTRATION(R.string.error_user_not_registered),
    USER_ALREADY_REGISTERED(R.string.error_user_already_registered),
    AUTH_REQUIRED(R.string.error_auth_required),
    INVALID_PASSWORD(R.string.error_incorrect_login_or_password),
    INVALID_PHOTO(R.string.error_invalid_photo_format),
    INVALID_FILE(R.string.error_invalid_attachment_format),
    EMPTY_CONTENT_OR_ATTACHMENT(R.string.error_empty_content_or_attachment),
    EMPTY_EVENT_DATE(R.string.error_empty_event_date),
}