package ru.netology.nework.model

data class AppModelState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val errorMessage: String? = null,
    val refreshing: Boolean = false,
)
