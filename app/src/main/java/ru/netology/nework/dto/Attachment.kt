package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class Attachment(
    @SerializedName("type")
    val type: AttachmentType,
    @SerializedName("url")
    val url: String
)

enum class AttachmentType {
    @SerializedName("IMAGE")
    IMAGE,
    @SerializedName("VIDEO")
    VIDEO,
    @SerializedName("AUDIO")
    AUDIO,
}