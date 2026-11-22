package ru.netology.nework.entity.user

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.Job

@Entity
data class JobEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val position: String,
    val start: String,
    val finish: String?,
    val link: String?,
    val ownerId: Long?
)

fun JobEntity.toDto() = Job(
    id = id,
    name = name,
    position = position,
    start = start,
    finish = finish,
    link = link,
    ownerId = ownerId
)

fun Job.toEntity(ownerId: Long) = JobEntity(
    id = id,
    name = name,
    position = position,
    start = start,
    finish = finish,
    link = link,
    ownerId = ownerId
)

fun List<Job>.toEntity(ownerId: Long): List<JobEntity> = map { it.toEntity(ownerId) }