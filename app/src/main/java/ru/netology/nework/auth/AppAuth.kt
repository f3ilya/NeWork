package ru.netology.nework.auth

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuth @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
    private val idKey = "id"
    private val tokenKey = "token"
    private val avatarKey = "avatar"
    private val _authStateFlow: MutableStateFlow<AuthState>

    init {
        val id = prefs.getLong(idKey, 0)
        val token = prefs.getString(tokenKey, null)
        val avatar = prefs.getString(avatarKey, null)

        if (id == 0L || token == null) {
            _authStateFlow = MutableStateFlow(AuthState())
            prefs.edit { clear() }
        } else {
            _authStateFlow = MutableStateFlow(AuthState(id, token, avatar))
        }
    }

    val authStateFlow: StateFlow<AuthState> = _authStateFlow.asStateFlow()

    @Synchronized
    fun setAuth(id: Long, token: String, avatar: String?) {
        Log.d("MyTagDebugAuth", token)
        _authStateFlow.value = AuthState(id, token, avatar)
        prefs.edit {
            putLong(idKey, id)
            putString(tokenKey, token)
            putString(avatarKey, avatar)
        }
    }

    @Synchronized
    fun removeAuth() {
        _authStateFlow.value = AuthState()
        prefs.edit(true) {
            clear()
        }
    }
}

data class AuthState(
    val id: Long = 0,
    val token: String? = null,
    val avatar: String? = null,
)