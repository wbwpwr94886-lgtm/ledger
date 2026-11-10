package com.example

import com.example.data.model.Account
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.data.model.Supplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSummaryCardsTest {

    @Test
    fun testCurrentBalancesCalculation() {
        val accounts = listOf(
            Account(id = 1, name = "الصندوق الرئيسي", code = "CASH-01", type = "CASH", balance = 2500000.0),
            Account(id = 2, name = "مصرف الرافدين", code = "BANK-01", type = "BANK", balance = 6000000.0),
            Account(id = 3, name = "زين كاش", code = "ZAIN-01", type = "WALLET", balance = 500000.0)
        )

        val totalTreasuryBalance = accounts.sumOf { it.balance }
        assertEquals(9000000.0, totalTreasuryBalance, 0.001)
        assertTrue(totalTreasuryBalance > 0)
    }

    @Test
    fun testDebtsAndReceivablesNetCalculation() {
        val customers = listOf(
            Customer(id = 1, name = "حيدر الكرخي", phone = "07701112233", currentDebt = 150000.0),
            Customer(id = 2, name = "أحمد السعدي", phone = "07802223344", currentDebt = 350000.0),
            Customer(id = 3, name = "ميثم علي", phone = "07709998877", currentDebt = 0.0)
        )
        val suppliers = listOf(
            Supplier(id = 1, name = "شركة النور للتجارة", phone = "07712345678", balanceOwed = 200000.0),
            Supplier(id = 2, name = "مذخر دجلة", phone = "07719998888", balanceOwed = 100000.0)
        )

        val totalCustomerDebt = customers.sumOf { it.currentDebt }
        val totalSupplierDebt = suppliers.sumOf { it.balanceOwed }
        val netDebtBalance = totalCustomerDebt - totalSupplierDebt

        assertEquals(500000.0, totalCustomerDebt, 0.001)
        assertEquals(300000.0, totalSupplierDebt, 0.001)
        assertEquals(200000.0, netDebtBalance, 0.001)
        assertTrue(netDebtBalance > 0) // Net receivable surplus
        assertEquals(2, customers.count { it.currentDebt > 0 })
        assertEquals(2, suppliers.count { it.balanceOwed > 0 })
    }

    @Test
    fun testInventoryStatusCalculations() {
        val products = listOf(
            Product(id = 1, name = "زيت محركات ديزل", category = "زيوت", buyPrice = 18000.0, sellPrice = 25000.0, stockQuantity = 30.0, minStockAlert = 5.0),
            Product(id = 2, name = "فلتر زيت كاتربيلر", category = "فلاتر", buyPrice = 12000.0, sellPrice = 18000.0, stockQuantity = 3.0, minStockAlert = 5.0),
            Product(id = 3, name = "حزام محرك", category = "قطع غيار", buyPrice = 8000.0, sellPrice = 14000.0, stockQuantity = 0.0, minStockAlert = 2.0),
            Product(id = 4, name = "قاطع دورة شنايدر", category = "كهربائيات", buyPrice = 25000.0, sellPrice = 35000.0, stockQuantity = 10.0, minStockAlert = 4.0)
        )

        val totalStockValuation = products.sumOf { it.stockQuantity * it.sellPrice }
        val totalStockCost = products.sumOf { it.stockQuantity * it.buyPrice }
        val totalUnits = products.sumOf { it.stockQuantity }
        val lowStockCount = products.count { it.stockQuantity <= it.minStockAlert && it.stockQuantity > 0 }
        val outOfStockCount = products.count { it.stockQuantity <= 0.0 }
        val healthyStockCount = products.count { it.stockQuantity > it.minStockAlert }

        // 30*25000 + 3*18000 + 0*14000 + 10*35000 = 750,000 + 54,000 + 0 + 350,000 = 1,154,000
        assertEquals(1154000.0, totalStockValuation, 0.001)
        assertEquals(43.0, totalUnits, 0.001)
        assertEquals(1, lowStockCount) // filter oil (qty 3 <= min 5)
        assertEquals(1, outOfStockCount) // belt (qty 0)
        assertEquals(2, healthyStockCount) // oil (30 > 5) & breaker (10 > 4)
    }
}
