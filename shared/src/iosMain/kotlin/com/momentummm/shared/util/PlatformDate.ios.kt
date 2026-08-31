package com.momentummm.shared.util

import kotlinx.datetime.LocalDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate

actual fun todayLocalDate(): LocalDate {
    val componentes = NSCalendar.currentCalendar.components(
        NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay,
        fromDate = NSDate(),
    )
    return LocalDate(
        year = componentes.year.toInt(),
        monthNumber = componentes.month.toInt(),
        dayOfMonth = componentes.day.toInt(),
    )
}
