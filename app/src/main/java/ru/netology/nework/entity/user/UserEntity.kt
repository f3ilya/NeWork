package ru.netology.nework.entity.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.UserResponse

@Entity
data class UserEntity(
    @PrimaryKey
    val id: Long,
    val login: String,
    val name: String,
    val avatar: String?,
)

fun UserEntity.toDto() = UserResponse(
    id = id,
    login = login,
    name = name,
    avatar = avatar
)

fun UserResponse.toEntity() = UserEntity(
    id = id,
    login = login,
    name = name,
    avatar = avatar
)


fun List<UserEntity>.toDto(): List<UserResponse> = map(UserEntity::toDto)
fun List<UserResponse>.toEntity(): List<UserEntity> = map(UserResponse::toEntity)
