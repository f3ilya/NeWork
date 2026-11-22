package ru.netology.nework.entity.event

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Attachment
import ru.netology.nework.dto.Coordinates
import ru.netology.nework.dto.Event
import ru.netology.nework.dto.EventType
import ru.netology.nework.dto.UserPreview

@Entity
data class EventEntity(
    @PrimaryKey
    val id: Long,
    val authorId: Long,
    val author: String,
    val authorJob: String?,
    val authorAvatar: String?,
    val content: String,
    val datetime: String,
    val published: String,
    @Embedded(prefix = "cords_")
    val coordinates: Coordinates?,
    val type: EventType,
    val likeOwnerIds: List<Long>,
    val likedByMe: Boolean,
    val speakerIds: List<Long>,
    val participantsIds: List<Long>,
    val participatedByMe: Boolean,
    @Embedded(prefix = "attach_")
    val attachment: Attachment?,
    val link: String?,
    val users: Map<String, UserPreview>,
)

fun EventEntity.toDto() = Event(
    id = id,
    authorId = authorId,
    author = author,
    authorJob = authorJob,
    authorAvatar = authorAvatar,
    content = content,
    datetime = datetime,
    published = published,
    coordinates = coordinates,
    type = type,
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    speakerIds = speakerIds,
    participantsIds = participantsIds,
    participatedByMe = participatedByMe,
    attachment = attachment,
    link = link,
    users = users
)

fun Event.toEntity() = EventEntity(
    id = id,
    authorId = authorId,
    author = author,
    authorJob = authorJob,
    authorAvatar = authorAvatar,
    content = content,
    datetime = datetime,
    published = published,
    coordinates = coordinates,
    type = type,
    likeOwnerIds = likeOwnerIds,
    likedByMe = likedByMe,
    speakerIds = speakerIds,
    participantsIds = participantsIds,
    participatedByMe = participatedByMe,
    attachment = attachment,
    link = link,
    users = users
)

fun List<Event>.toEntity(): List<EventEntity> = map(Event::toEntity)
