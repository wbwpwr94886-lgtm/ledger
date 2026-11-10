package com.example.util

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedAlert

/**
 * Validation result for financial and numerical input amounts
 */
sealed class ValidationResult {
    data class Valid(val value: Double) : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

object ValidationHelper {

    /**
     * Converts Eastern Arabic and Persian digits to standard Western Arabic (0-9) digits,
     * and normalizes commas to dots.
     */
    fun normalizeArabicDigits(input: String): String {
        if (input.isBlank()) return ""
        val sb = StringBuilder()
        for (ch in input.trim()) {
            when (ch) {
                '٠', '۰' -> sb.append('0')
                '١', '۱' -> sb.append('1')
                '٢', '۲' -> sb.append('2')
                '٣', '۳' -> sb.append('3')
                '٤', '۴' -> sb.append('4')
                '٥', '۵' -> sb.append('5')
                '٦', '۶' -> sb.append('6')
                '٧', '۷' -> sb.append('7')
                '٨', '۸' -> sb.append('8')
                '٩', '۹' -> sb.append('9')
                '٫', ',' -> sb.append('.')
                '٬' -> {} // Ignore Arabic thousands separator
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Validates that an amount string is a realistic non-negative or positive number.
     * Prevents text, symbols, multiple decimals, and negative numbers.
     */
    fun validateAmount(
        input: String,
        allowZero: Boolean = false,
        maxAmount: Double = 1_000_000_000.0,
        isAr: Boolean = true
    ): ValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return if (allowZero) {
                ValidationResult.Valid(0.0)
            } else {
                ValidationResult.Invalid(if (isAr) "يرجى إدخال المبلغ" else "Please enter an amount")
            }
        }

        // Explicit check for negative sign or letters
        if (trimmed.contains('-')) {
            return ValidationResult.Invalid(if (isAr) "غير مسموح بقيم سالبة" else "Negative values are not allowed")
        }

        val normalized = normalizeArabicDigits(trimmed)
        val parsed = normalized.toDoubleOrNull()
            ?: return ValidationResult.Invalid(if (isAr) "قيمة غير صالحة (أرقام فقط)" else "Invalid number (digits only)")

        if (parsed.isNaN() || parsed.isInfinite()) {
            return ValidationResult.Invalid(if (isAr) "قيمة غير صالحة" else "Invalid value")
        }

        if (parsed < 0.0) {
            return ValidationResult.Invalid(if (isAr) "غير مسموح بقيم سالبة" else "Negative values are not allowed")
        }

        if (parsed == 0.0 && !allowZero) {
            return ValidationResult.Invalid(if (isAr) "يجب أن يكون المبلغ أكبر من صفر" else "Amount must be greater than 0")
        }

        if (parsed > maxAmount) {
            return ValidationResult.Invalid(if (isAr) "المبلغ كبير جداً وغير منطقي" else "Amount is unrealistically large")
        }

        return ValidationResult.Valid(parsed)
    }

    /**
     * Safely parse a valid amount, returning null if invalid or negative
     */
    fun parseValidAmount(input: String, allowZero: Boolean = false): Double? {
        return when (val res = validateAmount(input, allowZero = allowZero)) {
            is ValidationResult.Valid -> res.value
            is ValidationResult.Invalid -> null
        }
    }

    /**
     * Returns error message if input is invalid, or null if valid/empty-allowed
     */
    fun getAmountError(input: String, allowZero: Boolean = false, isAr: Boolean = true): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return if (allowZero) null else (if (isAr) "حقل إلزامي" else "Required field")
        }
        return when (val res = validateAmount(input, allowZero = allowZero, isAr = isAr)) {
            is ValidationResult.Valid -> null
            is ValidationResult.Invalid -> res.message
        }
    }
}

/**
 * Reusable Material 3 Amount OutlinedTextField with real-time validation,
 * decimal numeric keypad, Arabic digit normalization, and error messaging.
 */
@Composable
fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    allowZero: Boolean = false,
    maxAmount: Double = 1_000_000_000.0,
    isAr: Boolean = true,
    currencySymbol: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    colors: TextFieldColors? = null
) {
    val errorText = remember(value, allowZero, isAr) {
        if (value.isBlank() && allowZero) {
            null
        } else if (value.isBlank()) {
            null // Don't show red error on fresh empty field until typed
        } else {
            ValidationHelper.getAmountError(value, allowZero = allowZero, isAr = isAr)
        }
    }

    val isError = errorText != null

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            // Prevent users from typing multiple decimal dots or invalid characters directly
            val normalized = ValidationHelper.normalizeArabicDigits(raw)
            // Allow typing empty, single dot, or numbers with at most one dot
            val dotCount = normalized.count { it == '.' }
            val hasInvalidChars = normalized.any { !it.isDigit() && it != '.' && it != '-' }
            if (dotCount <= 1 && !hasInvalidChars) {
                onValueChange(raw)
            }
        },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = {
            if (isError && errorText != null) {
                Text(
                    text = "⚠️ $errorText",
                    color = RedAlert,
                    fontSize = 11.sp
                )
            }
        },
        trailingIcon = {
            if (isError) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = RedAlert,
                    modifier = Modifier.size(20.dp)
                )
            } else if (!currencySymbol.isNullOrBlank()) {
                Text(
                    text = currencySymbol,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal
        ),
        singleLine = singleLine,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = colors ?: OutlinedTextFieldDefaults.colors()
    )
}
