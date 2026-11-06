package ru.netology.nework.entity.user

import androidx.room.Entity

@Entity(primaryKeys = ["type", "authorId"])
data class WallRemoteKeyEntity(
    val type: KeyType,
    val authorId: Long,
    val id: Long
) {
    enum class KeyType {
        AFTER, BEFORE
    }
}
