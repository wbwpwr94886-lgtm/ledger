package com.example.util

import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class AppLanguage(val code: String, val displayName: String) {
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English")
}

enum class AppCurrency(val code: String, val symbolAr: String, val symbolEn: String) {
    IQD("IQD", "د.ع", "IQD"),
    USD("USD", "$", "$")
}

object LanguageManager {
    fun formatMoney(amount: Double, currency: AppCurrency, isArabic: Boolean): String {
        val formatter = DecimalFormat("#,##0.##")
        val formattedNumber = formatter.format(amount)
        return if (isArabic) {
            "$formattedNumber ${currency.symbolAr}"
        } else {
            "${currency.symbolEn} $formattedNumber"
        }
    }

    fun formatDate(millis: Long, isArabic: Boolean): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val sdf = SimpleDateFormat("yyyy/MM/dd - hh:mm a", locale)
        return sdf.format(Date(millis))
    }

    fun formatDateShort(millis: Long, isArabic: Boolean): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        val sdf = SimpleDateFormat("yyyy/MM/dd", locale)
        return sdf.format(Date(millis))
    }
}
