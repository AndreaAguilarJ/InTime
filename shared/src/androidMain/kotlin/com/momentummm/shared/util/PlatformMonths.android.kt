package com.momentummm.shared.util

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

actual fun shortMonthNames(): List<String> =
    (1..12).map { numero ->
        Month.of(numero).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    }
