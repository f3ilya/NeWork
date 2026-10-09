package ru.netology.nework.dto


import com.google.gson.annotations.SerializedName

data class Post(
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
    @SerializedName("published")
    val published: String,
    @SerializedName("coordinates")
    val coordinates: Coordinates? = null,
    @SerializedName("link")
    val link: String? = null,
    @SerializedName("mentionIds")
    val mentionIds: List<Long>,
    @SerializedName("mentionedMe")
    val mentionedMe: Boolean,
    @SerializedName("likeOwnerIds")
    val likeOwnerIds: List<Long>,
    @SerializedName("likedByMe")
    val likedByMe: Boolean,
    @SerializedName("attachment")
    val attachment: Attachment? = null,
    @SerializedName("users")
    val users: Map<String, UserPreview>,
    val ownedByMe: Boolean = false,
)