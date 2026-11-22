package ru.netology.nework.util

import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class DateFormatPattern(pattern: String) {
    DATE_TIME("dd.MM.yyyy HH:mm"),
    SHORT_DATE("dd.MM.yyyy"),
    FULL_MONTH_DATE("d MMMM yyyy");

    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
        .withZone(ZoneId.systemDefault())
}