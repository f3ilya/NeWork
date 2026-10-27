package ru.netology.nework.model

import ru.netology.nework.dto.UserResponse

data class UserModel(
    val user: UserResponse,
    val isSelected: Boolean = false
)
