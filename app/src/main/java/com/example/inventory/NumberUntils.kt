package com.example.inventory

import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter



object NumberUtils {
    fun formatNumberPrice(input: String): String {
        // Chuyển chuỗi input thành số
        val number = input.toDoubleOrNull() ?: return "Invalid input"

        // Sử dụng DecimalFormat để định dạng số
        val decimalFormat = DecimalFormat("#,##0.00")
        return decimalFormat.format(number)
    }
}
