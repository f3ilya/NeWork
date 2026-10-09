package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class Coordinates(
    @SerializedName("lat")
    val lat: Double,
    @SerializedName("long")
    val long: Double
)