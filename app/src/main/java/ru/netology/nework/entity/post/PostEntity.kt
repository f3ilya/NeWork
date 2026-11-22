package ru.netology.nework.entity.post

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserPreview

@Entity
data class PostEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String?,
    val authorAvatar: String?,
    val content: String,
    val published: String,
    @Embedded(prefix = "cords_")
    val coordinates: Coordinates?,
    val link: String?,
    val mentionIds: List<Long>,
    val mentionedMe: Boolean,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    @Embedded(prefix = "attach_")
    val attachment: Attachment?,
    val users: Map<String, UserPreview>
)

fun PostEntity.toDto() = Post(
    id = id,
    authorId = authorId,
    author = author,
    authorJob = authorJob,
    authorAvatar = authorAvatar,
    content = content,
    published = published,
    coordinates = coordinates,
    link = link,
    mentionIds = mentionIds,
    mentionedMe = mentionedMe,
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    attachment = attachment,
    users = users
)

fun Post.toEntity() = PostEntity(
    id = id,
    authorId = authorId,
    author = author,
    authorJob = authorJob,
    authorAvatar = authorAvatar,
    content = content,
    published = published,
    coordinates = coordinates,
    link = link,
    mentionIds = mentionIds,
    mentionedMe = mentionedMe,
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    attachment = attachment,
    users = users
)

fun List<Post>.toEntity(): List<PostEntity> = map(Post::toEntity)