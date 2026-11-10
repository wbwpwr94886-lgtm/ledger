package com.example.util

import org.junit.Assert.*
import org.junit.Test

class ValidationHelperTest {

    @Test
    fun testNormalizeArabicDigits() {
        assertEquals("12345.67", ValidationHelper.normalizeArabicDigits("١٢٣٤٥.٦٧"))
        assertEquals("100.5", ValidationHelper.normalizeArabicDigits("۱۰۰٫۵"))
        assertEquals("5000", ValidationHelper.normalizeArabicDigits("5٬000"))
    }

    @Test
    fun testValidateAmountValid() {
        val res = ValidationHelper.validateAmount("150.50", allowZero = false)
        assertTrue(res is ValidationResult.Valid)
        assertEquals(150.50, (res as ValidationResult.Valid).value, 0.001)

        val resArabic = ValidationHelper.validateAmount("٢٥٠", allowZero = false)
        assertTrue(resArabic is ValidationResult.Valid)
        assertEquals(250.0, (resArabic as ValidationResult.Valid).value, 0.001)
    }

    @Test
    fun testValidateAmountZero() {
        val notAllowed = ValidationHelper.validateAmount("0", allowZero = false)
        assertTrue(notAllowed is ValidationResult.Invalid)

        val allowed = ValidationHelper.validateAmount("0", allowZero = true)
        assertTrue(allowed is ValidationResult.Valid)
        assertEquals(0.0, (allowed as ValidationResult.Valid).value, 0.001)

        val emptyAllowed = ValidationHelper.validateAmount("", allowZero = true)
        assertTrue(emptyAllowed is ValidationResult.Valid)
        assertEquals(0.0, (emptyAllowed as ValidationResult.Valid).value, 0.001)
    }

    @Test
    fun testValidateAmountNegative() {
        val res = ValidationHelper.validateAmount("-50")
        assertTrue(res is ValidationResult.Invalid)
        val msg = (res as ValidationResult.Invalid).message
        assertTrue(msg.contains("سالبة") || msg.contains("Negative"))
    }

    @Test
    fun testValidateAmountInvalidText() {
        val res = ValidationHelper.validateAmount("abc")
        assertTrue(res is ValidationResult.Invalid)

        val resSymbols = ValidationHelper.validateAmount("12.34.56")
        assertTrue(resSymbols is ValidationResult.Invalid)
    }
}
