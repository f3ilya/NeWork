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
) {
    fun toDto() = Job(
        id = id,
        name = name,
        position = position,
        start = start,
        finish = finish,
        link = link,
        ownerId = ownerId
    )

    companion object {
        fun fromDto(dto: Job, ownerId: Long) = JobEntity(
            id = dto.id,
            name = dto.name,
            position = dto.position,
            start = dto.start,
            finish = dto.finish,
            link = dto.link,
            ownerId = ownerId
        )
    }
}

fun List<JobEntity>.toDto(): List<Job> = map(JobEntity::toDto)
fun List<Job>.toEntity(ownerId: Long): List<JobEntity> = map { JobEntity.fromDto(it, ownerId) }