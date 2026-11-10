package com.example

import com.example.data.model.Account
import com.example.data.model.DebtRecord
import com.example.data.model.Product
import com.example.util.DashboardSearchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSearchFilterTest {

    private val sampleAccounts = listOf(
        Account(id = 1, name = "الصندوق النقدي الرئيسي", code = "CASH-01", type = "CASH", balance = 1500000.0),
        Account(id = 2, name = "حساب مصرف الرافدين", code = "BANK-01", type = "BANK", balance = 5000000.0),
        Account(id = 3, name = "محفظة زين كاش", code = "ZAIN-01", type = "WALLET", balance = 750000.0),
        Account(id = 4, name = "USD Petty Cash", code = "USD-01", type = "CASH", balance = 1200.0)
    )

    private val sampleDebts = listOf(
        DebtRecord(id = 1, debtorName = "حيدر الكرخي", debtType = "RECEIVABLE", remainingAmount = 45000.0, debtorPhone = "07701112233"),
        DebtRecord(id = 2, debtorName = "علي السعدي", debtType = "RECEIVABLE", remainingAmount = 90000.0, debtorPhone = "07802223344"),
        DebtRecord(id = 3, debtorName = "شركة النور للمقاولات", debtType = "PAYABLE", remainingAmount = 250000.0, debtorPhone = "07709998877"),
        DebtRecord(id = 4, debtorName = "John Miller Supplier", debtType = "PAYABLE", remainingAmount = 500.0, debtorPhone = "07712345678")
    )

    private val sampleProducts = listOf(
        Product(id = 1, name = "زيت محركات ديزل 15W40", category = "زيوت ومحركات", sellPrice = 25000.0, stockQuantity = 38.0),
        Product(id = 2, name = "فلتر ديزل أصلي كاتربيلر", category = "فلاتر", sellPrice = 18000.0, stockQuantity = 12.0),
        Product(id = 3, name = "قاطع دورة شنايدر 63 أمبير", category = "كهربائيات", sellPrice = 35000.0, stockQuantity = 4.0),
        Product(id = 4, name = "Diesel Engine Belt", category = "Spare Parts", sellPrice = 15000.0, stockQuantity = 20.0)
    )

    @Test
    fun testEmptyOrBlankSearchReturnsEmptyResults() {
        val emptyResult = DashboardSearchHelper.filter("", sampleAccounts, sampleDebts, sampleProducts)
        assertTrue(emptyResult.isEmpty)
        assertFalse(emptyResult.hasMatches)
        assertEquals(0, emptyResult.totalCount)

        val whitespaceResult = DashboardSearchHelper.filter("   ", sampleAccounts, sampleDebts, sampleProducts)
        assertTrue(whitespaceResult.isEmpty)
        assertEquals(0, whitespaceResult.totalCount)
    }

    @Test
    fun testFilterAccountsByName() {
        val result = DashboardSearchHelper.filter("الرافدين", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(1, result.matchingAccounts.size)
        assertEquals("حساب مصرف الرافدين", result.matchingAccounts[0].name)
        assertEquals(0, result.matchingDebts.size)
        assertEquals(0, result.matchingProducts.size)
        assertEquals(1, result.totalCount)
    }

    @Test
    fun testFilterDebtsByName() {
        val result = DashboardSearchHelper.filter("حيدر", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(0, result.matchingAccounts.size)
        assertEquals(1, result.matchingDebts.size)
        assertEquals("حيدر الكرخي", result.matchingDebts[0].debtorName)
        assertEquals(0, result.matchingProducts.size)
        assertEquals(1, result.totalCount)
    }

    @Test
    fun testFilterInventoryProductsByName() {
        val result = DashboardSearchHelper.filter("فلتر", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(0, result.matchingAccounts.size)
        assertEquals(0, result.matchingDebts.size)
        assertEquals(1, result.matchingProducts.size)
        assertEquals("فلتر ديزل أصلي كاتربيلر", result.matchingProducts[0].name)
        assertEquals(1, result.totalCount)
    }

    @Test
    fun testCrossEntitySearchQueryMatchingMultipleCategories() {
        // "ديزل" matches both "زيت محركات ديزل 15W40", "فلتر ديزل أصلي كاتربيلر", and "Diesel Engine Belt" (when case-insensitive English)
        val result = DashboardSearchHelper.filter("ديزل", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(0, result.matchingAccounts.size)
        assertEquals(0, result.matchingDebts.size)
        assertEquals(2, result.matchingProducts.size)

        // English search test: "Diesel"
        val englishResult = DashboardSearchHelper.filter("diesel", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(1, englishResult.matchingProducts.size)
        assertEquals("Diesel Engine Belt", englishResult.matchingProducts[0].name)
    }

    @Test
    fun testSearchCaseInsensitivity() {
        val upperResult = DashboardSearchHelper.filter("PETTY", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(1, upperResult.matchingAccounts.size)
        assertEquals("USD Petty Cash", upperResult.matchingAccounts[0].name)

        val lowerResult = DashboardSearchHelper.filter("petty", sampleAccounts, sampleDebts, sampleProducts)
        assertEquals(1, lowerResult.matchingAccounts.size)
        assertEquals("USD Petty Cash", lowerResult.matchingAccounts[0].name)
    }

    @Test
    fun testNonMatchingSearchReturnsZeroMatches() {
        val result = DashboardSearchHelper.filter("غير موجود نهائيا XYZ123", sampleAccounts, sampleDebts, sampleProducts)
        assertTrue(result.isEmpty)
        assertFalse(result.hasMatches)
        assertEquals(0, result.totalCount)
        assertEquals(0, result.matchingAccounts.size)
        assertEquals(0, result.matchingDebts.size)
        assertEquals(0, result.matchingProducts.size)
    }
}
