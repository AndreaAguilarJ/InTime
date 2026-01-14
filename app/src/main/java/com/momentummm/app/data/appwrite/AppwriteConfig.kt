package com.momentummm.app.data.appwrite

object AppwriteConfig {
    const val APPWRITE_PROJECT_ID = "momentum-intime"
    const val APPWRITE_PROJECT_NAME = "momentum-intime"
    const val APPWRITE_PUBLIC_ENDPOINT = "https://sfo.cloud.appwrite.io/v1"
    const val DATABASE_ID = "momentum-db"
    
    // Aliases para compatibilidad con AppwriteService y otros usos
    const val ENDPOINT: String = APPWRITE_PUBLIC_ENDPOINT
    const val PROJECT_ID: String = APPWRITE_PROJECT_ID

    // Collection IDs
    const val USERS_COLLECTION_ID = "users"
    const val QUOTES_COLLECTION_ID = "quotes"
    const val USER_SETTINGS_COLLECTION_ID = "user_settings"
    const val APP_USAGE_COLLECTION_ID = "app_usage"
    const val FOCUS_SESSIONS_COLLECTION_ID = "focus_sessions"

    // NYC Server configuration
    const val SERVER_REGION = "nyc1"
}