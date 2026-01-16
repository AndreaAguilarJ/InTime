package com.momentummm.app.data.appwrite

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import io.appwrite.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppwriteService(context: Context) {
    private val client = Client(context)
        .setEndpoint(AppwriteConfig.ENDPOINT)
        .setProject(AppwriteConfig.PROJECT_ID)
    
    val account = Account(client)
    val databases = Databases(client)
    val storage = Storage(client)
    val databaseId = AppwriteConfig.DATABASE_ID

    private val _currentUser = MutableStateFlow<User<*>?>(null)
    val currentUser: StateFlow<User<*>?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // Indica que la verificación inicial de sesión terminó (éxito o fallo)
    private val _isAuthReady = MutableStateFlow(false)
    val isAuthReady: StateFlow<Boolean> = _isAuthReady.asStateFlow()

    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        _isAuthReady.value = false
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = account.get()
                _currentUser.value = user
                _isLoggedIn.value = true
            } catch (e: Exception) {
                _currentUser.value = null
                _isLoggedIn.value = false
            } finally {
                _isAuthReady.value = true
            }
        }
    }
    
    suspend fun createAccount(email: String, password: String, name: String): Result<User<*>> {
        return try {
            account.create(
                userId = "unique()",
                email = email,
                password = password,
                name = name
            )
            // Crear sesión tras crear cuenta
            runCatching { account.createEmailPasswordSession(email, password) }
            val user = account.get()
            _currentUser.value = user
            _isLoggedIn.value = true
            _isAuthReady.value = true
            Result.success(user)
        } catch (e: Exception) {
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<User<*>> {
        return try {
            account.createEmailPasswordSession(email, password)
            val user = account.get()
            _currentUser.value = user
            _isLoggedIn.value = true
            _isAuthReady.value = true
            Result.success(user)
        } catch (e: Exception) {
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            account.deleteSession("current")
            _currentUser.value = null
            _isLoggedIn.value = false
            _isAuthReady.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): Result<User<*>> {
        return try {
            val user = account.get()
            _currentUser.value = user
            _isLoggedIn.value = true
            _isAuthReady.value = true
            Result.success(user)
        } catch (e: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
            _isAuthReady.value = true
            Result.failure(e)
        }
    }
}