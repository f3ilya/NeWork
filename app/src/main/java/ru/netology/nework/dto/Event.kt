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
    val authorJob: String,
    @SerializedName("authorAvatar")
    val authorAvatar: String,
    @SerializedName("content")
    val content: String,
    @SerializedName("datetime")
    val datetime: String,
    @SerializedName("published")
    val published: String,
    @SerializedName("coordinates")
    val coordinates: Coordinates,
    @SerializedName("type")
    val type: String,
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
    val attachment: Attachment,
    @SerializedName("link")
    val link: String,
    @SerializedName("users")
    val users: List<UserPreview>,
)

enum class EventsType {
    @SerializedName("OFFLINE")
    OFFLINE,
    @SerializedName("ONLINE")
    ONLINE,
}
