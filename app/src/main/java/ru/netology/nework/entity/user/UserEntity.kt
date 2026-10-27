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
) {
    fun toDto() = UserResponse(
        id = id,
        login = login,
        name = name,
        avatar = avatar
    )

    companion object {
        fun fromDto(dto: UserResponse) = UserEntity(
            id = dto.id,
            login = dto.login,
            name = dto.name,
            avatar = dto.avatar
        )
    }
}

fun List<UserEntity>.toDto(): List<UserResponse> = map(UserEntity::toDto)
fun List<UserResponse>.toEntity(): List<UserEntity> = map(UserEntity::fromDto)
