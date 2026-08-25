package com.momentummm.app.ui.screen.categories

import android.content.Context
import com.momentummm.app.R
import com.momentummm.app.data.entity.AppCategory

/**
 * Traduce el nombre y la descripción de las seis categorías del sistema.
 *
 * Por qué al mostrar y no al guardar: AppCategory.SYSTEM_CATEGORIES se inserta en la base
 * de datos UNA sola vez, en la primera ejecución (AppCategoryRepository.initializeSystemCategories).
 * Si se tradujera al insertar, la base de datos quedaría congelada en el idioma que
 * hubiera activo ese día, y cambiar de idioma después no cambiaría nada. Resolviéndolo al
 * mostrar, el idioma sigue al usuario.
 *
 * Ventaja añadida: no hace falta migración de Room ni tocar el esquema, y funciona también
 * para las instalaciones que ya tienen las categorías guardadas en español.
 *
 * Las categorías creadas por el usuario se devuelven tal cual: su nombre es suyo y no se
 * traduce.
 */
object SystemCategoryLabels {

    private val nombres = mapOf(
        "Social" to R.string.syscat_social,
        "Entretenimiento" to R.string.syscat_entertainment,
        "Juegos" to R.string.syscat_games,
        "Productividad" to R.string.syscat_productivity,
        "Noticias" to R.string.syscat_news,
        "Compras" to R.string.syscat_shopping
    )

    private val descripciones = mapOf(
        "Redes sociales y mensajería" to R.string.syscat_social_desc,
        "Videos, streaming y música" to R.string.syscat_entertainment_desc,
        "Juegos y entretenimiento interactivo" to R.string.syscat_games_desc,
        "Trabajo y herramientas" to R.string.syscat_productivity_desc,
        "Noticias y actualidad" to R.string.syscat_news_desc,
        "E-commerce y compras" to R.string.syscat_shopping_desc
    )

    /** Nombre traducido si es una categoría del sistema; el propio nombre si es del usuario. */
    fun name(context: Context, category: AppCategory): String =
        if (category.isSystemCategory) {
            nombres[category.name]?.let { context.getString(it) } ?: category.name
        } else {
            category.name
        }

    /** Descripción traducida si es del sistema; la propia si es del usuario. */
    fun description(context: Context, category: AppCategory): String =
        if (category.isSystemCategory) {
            descripciones[category.description]?.let { context.getString(it) } ?: category.description
        } else {
            category.description
        }
}
