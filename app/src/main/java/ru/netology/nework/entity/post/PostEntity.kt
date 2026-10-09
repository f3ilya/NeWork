package ru.netology.nework.entity.post

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Post
import ru.netology.nework.dto.UserPreview

@Entity(tableName = "posts")
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
    val attachment: Attachment?,
    val users: Map<String, UserPreview>
) {
    fun toDto() = Post(
        id = id,
        authorId = authorId,
        author = author,
        authorJob = authorJob.orEmpty(),
        authorAvatar = authorAvatar.orEmpty(),
        content = content,
        published = published,
        coordinates = coordinates ?: Coordinates(Double.NaN, Double.NaN),
        link = link.orEmpty(),
        mentionIds = mentionIds,
        mentionedMe = mentionedMe,
        likeOwnerIds = likeOwnerIds,
        likedByMe = likedByMe,
        attachment = attachment,
        users = users
    )

    companion object {
        fun fromDto(dto: Post) = PostEntity(
            id = dto.id,
            authorId = dto.authorId,
            author = dto.author,
            authorJob = dto.authorJob,
            authorAvatar = dto.authorAvatar,
            content = dto.content,
            published = dto.published,
            coordinates = dto.coordinates,
            link = dto.link,
            mentionIds = dto.mentionIds,
            mentionedMe = dto.mentionedMe,
            likeOwnerIds = dto.likeOwnerIds,
            likedByMe = dto.likedByMe,
            attachment = dto.attachment,
            users = dto.users
        )
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)