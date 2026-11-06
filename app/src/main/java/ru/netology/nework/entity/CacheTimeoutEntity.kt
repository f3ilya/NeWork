package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CacheTimeoutEntity(
    @PrimaryKey val type: Type,
    val lastUpdated: Long
) {
    enum class Type {
        POSTS, EVENTS
    }
}
