package com.momentummm.shared.util

import kotlinx.datetime.LocalDate

actual fun todayLocalDate(): LocalDate {
    val hoy = java.time.LocalDate.now()
    return LocalDate(hoy.year, hoy.monthValue, hoy.dayOfMonth)
}
