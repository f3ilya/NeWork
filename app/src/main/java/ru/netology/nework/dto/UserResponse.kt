package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("id")
    val id: Long,
    @SerializedName("login")
    val login: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar")
    val avatar: String,
)