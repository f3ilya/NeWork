package ru.netology.nework.extensions

import ru.netology.nework.util.DateFormatPattern
import java.time.Instant
import java.util.Locale

fun String?.formatToLocalDateTime(pattern: DateFormatPattern): String? {
    if (this.isNullOrBlank()) return null
    return try {
        val instant = Instant.parse(this)
        pattern.formatter
            .withLocale(Locale.getDefault())
            .format(instant)
    } catch (_: Exception) {
        this
    }
}