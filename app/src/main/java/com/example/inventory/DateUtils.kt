package com.example.inventory

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {
    fun convertMillisToDateModern(millis: Long): String {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(millis))
    }
}
