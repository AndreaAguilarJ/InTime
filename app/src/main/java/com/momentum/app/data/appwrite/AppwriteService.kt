package com.momentum.app.data.appwrite

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
    
    private val _currentUser = MutableStateFlow<User<*>?>(null)
    val currentUser: StateFlow<User<*>?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    init {
        checkCurrentUser()
    }
    
    private fun checkCurrentUser() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = account.get()
                _currentUser.value = user
                _isLoggedIn.value = true
            } catch (e: Exception) {
                _currentUser.value = null
                _isLoggedIn.value = false
            }
        }
    }
    
    suspend fun createAccount(email: String, password: String, name: String): Result<User<*>> {
        return try {
            val user = account.create(
                userId = "unique()",
                email = email,
                password = password,
                name = name
            )
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(email: String, password: String): Result<User<*>> {
        return try {
            account.createEmailSession(email, password)
            val user = account.get()
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return try {
            account.deleteSession("current")
            _currentUser.value = null
            _isLoggedIn.value = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getCurrentUser(): Result<User<*>> {
        return try {
            val user = account.get()
            _currentUser.value = user
            _isLoggedIn.value = true
            Result.success(user)
        } catch (e: Exception) {
            _currentUser.value = null
            _isLoggedIn.value = false
            Result.failure(e)
        }
    }
}