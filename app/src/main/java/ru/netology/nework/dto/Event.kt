package ru.netology.nework.dto

import com.google.gson.annotations.SerializedName

data class Event(
    @SerializedName("id")
    val id: Long,
    @SerializedName("authorId")
    val authorId: Long,
    @SerializedName("author")
    val author: String,
    @SerializedName("authorJob")
    val authorJob: String? = null,
    @SerializedName("authorAvatar")
    val authorAvatar: String? = null,
    @SerializedName("content")
    val content: String,
    @SerializedName("datetime")
    val datetime: String,
    @SerializedName("published")
    val published: String,
    @Suppress("SpellCheckingInspection") @SerializedName("coords")
    val coordinates: Coordinates? = null,
    @SerializedName("type")
    val type: EventType,
    @SerializedName("likeOwnerIds")
    val likeOwnerIds: List<Long>,
    @SerializedName("likedByMe")
    val likedByMe: Boolean,
    @SerializedName("speakerIds")
    val speakerIds: List<Long>,
    @SerializedName("participantsIds")
    val participantsIds: List<Long>,
    @SerializedName("participatedByMe")
    val participatedByMe: Boolean,
    @SerializedName("attachment")
    val attachment: Attachment? = null,
    @SerializedName("link")
    val link: String? = null,
    @SerializedName("users")
    val users: Map<String, UserPreview>,
    val ownedByMe: Boolean = false,
)

enum class EventType {
    @SerializedName("OFFLINE")
    OFFLINE,

    @SerializedName("ONLINE")
    ONLINE,
}
