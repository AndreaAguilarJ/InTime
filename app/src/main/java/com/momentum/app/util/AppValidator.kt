package com.momentum.app.util

import android.content.Context

object AppValidator {
    
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
    
    fun validateAppIntegrity(context: Context): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Check if critical classes exist
        try {
            Class.forName("com.momentum.app.MomentumApplication")
        } catch (e: ClassNotFoundException) {
            errors.add("Clase principal de aplicación no encontrada")
        }
        
        try {
            Class.forName("com.momentum.app.data.AppDatabase")
        } catch (e: ClassNotFoundException) {
            errors.add("Base de datos no configurada correctamente")
        }
        
        try {
            Class.forName("com.momentum.app.data.appwrite.AppwriteService")
        } catch (e: ClassNotFoundException) {
            warnings.add("Servicio Appwrite no disponible - funciones en la nube limitadas")
        }
        
        // Check network connectivity for cloud features
        if (!NetworkUtils.isNetworkAvailable(context)) {
            warnings.add("Sin conexión a internet - funciones en la nube no disponibles")
        }
        
        // Check required permissions
        val packageManager = context.packageManager
        val packageName = context.packageName
        
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            // Basic validation passed
        } catch (e: Exception) {
            errors.add("Información del paquete no disponible")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    fun getRecommendations(validationResult: ValidationResult): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (!validationResult.isValid) {
            recommendations.add("Reinstala la aplicación para corregir archivos faltantes")
            recommendations.add("Verifica que tienes suficiente espacio de almacenamiento")
        }
        
        if (validationResult.warnings.any { it.contains("conexión") }) {
            recommendations.add("Conecta a internet para acceder a todas las funciones")
            recommendations.add("Algunas funciones trabajarán sin conexión")
        }
        
        if (validationResult.warnings.any { it.contains("Appwrite") }) {
            recommendations.add("Funciones de sincronización no estarán disponibles")
            recommendations.add("Los datos se guardarán solo localmente")
        }
        
        return recommendations
    }
}