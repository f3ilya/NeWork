package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class UserPreview(
    @SerializedName("name")
    val name: String,
    @SerializedName("avatar")
    val avatar: String? = null,
)