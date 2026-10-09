package ru.netology.nework.repository.auth

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nework.api.ApiService
import ru.netology.nework.auth.AppAuth
import ru.netology.nework.extensions.getOrThrow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val appAuth: AppAuth,
) : AuthRepository {
    override suspend fun registration(login: String, pass: String, name: String, file: File?) {
        val response = if (file == null) {
            apiService.registration(login, pass, name)
        } else {
            apiService.registrationWithAvatar(
                login = login.toRequestBody("text/plain".toMediaType()),
                pass = pass.toRequestBody("text/plain".toMediaType()),
                name = name.toRequestBody("text/plain".toMediaType()),
                MultipartBody.Part.createFormData(
                    "file", file.name, file.asRequestBody()
                )
            )
        }
        val token = response.getOrThrow()
        appAuth.setAuth(token.id, token.token, token.avatar)
    }

    override suspend fun authentication(login: String, pass: String) {
        val response = apiService.authentication(login, pass)
        val token = response.getOrThrow()
        appAuth.setAuth(token.id, token.token, token.avatar)
    }
}