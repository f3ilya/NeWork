package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class Token(
    @SerializedName("id")
    val id: Long,
    @SerializedName("token")
    val token: String,
    @SerializedName("avatar")
    val avatar: String? = null,
)
