package ru.netology.nework.repository.auth

import java.io.File

interface AuthRepository {
    suspend fun registration(login: String, pass: String, name: String, file: File?)
    suspend fun authentication(login: String, pass: String)
}