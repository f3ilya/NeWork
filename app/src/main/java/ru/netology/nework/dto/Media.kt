package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class Media(
    @SerializedName("url")
    val url: String,
)
