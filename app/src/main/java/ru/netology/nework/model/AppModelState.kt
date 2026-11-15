package ru.netology.nework.model

import ru.netology.nework.error.ErrorType

data class AppModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val errorType: ErrorType? = null,
    val refreshing: Boolean = false,
)
