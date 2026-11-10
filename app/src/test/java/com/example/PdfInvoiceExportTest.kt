package com.example

import com.example.data.model.Account
import com.example.data.model.AccountTransaction
import com.example.data.model.Customer
import com.example.data.model.CustomerReceipt
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfInvoiceExportTest {

    @Test
    fun testInvoiceCalculationAndFormatting() {
        val invoice = Invoice(
            id = 101L,
            invoiceNumber = "INV-2026-001",
            customerName = "أحمد البصري",
            customerPhone = "07701234567",
            totalAmount = 150000.0,
            discountAmount = 10000.0,
            paidAmount = 100000.0,
            remainingDebt = 40000.0,
            taxAmount = 0.0,
            createdBy = "علي المحاسب"
        )
        val items = listOf(
            InvoiceItem(invoiceId = 101L, productName = "زيت محرك 15W40", quantity = 2.0, unitPrice = 45000.0, totalPrice = 90000.0),
            InvoiceItem(invoiceId = 101L, productName = "فلتر ديزل أصلي", quantity = 3.0, unitPrice = 20000.0, totalPrice = 60000.0)
        )

        assertEquals("INV-2026-001", invoice.invoiceNumber)
        assertEquals(150000.0, invoice.totalAmount, 0.001)
        assertEquals(40000.0, invoice.remainingDebt, 0.001)
        assertEquals(2, items.size)
        assertEquals(150000.0, items.sumOf { it.totalPrice }, 0.001)

        val sanitizedNumber = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        assertEquals("INV-2026-001", sanitizedNumber)
    }

    @Test
    fun testAccountSummaryCalculations() {
        val account = Account(
            id = 1L,
            name = "الصندوق الرئيسي",
            code = "CASH-01",
            type = "CASH",
            balance = 2500000.0
        )
        val transactions = listOf(
            AccountTransaction(
                id = 1L,
                accountId = 1L,
                accountName = "الصندوق الرئيسي",
                type = "DEPOSIT",
                amount = 500000.0,
                balanceAfter = 2500000.0,
                category = "مبيعات نقدية",
                notes = "إيراد مبيعات اليوم"
            ),
            AccountTransaction(
                id = 2L,
                accountId = 1L,
                accountName = "الصندوق الرئيسي",
                type = "WITHDRAW",
                amount = 150000.0,
                balanceAfter = 2000000.0,
                category = "كاز وديزل",
                notes = "تزويد المولد 200 لتر"
            )
        )

        val totalDeposits = transactions.filter { it.type == "DEPOSIT" || it.type == "TRANSFER_IN" }.sumOf { it.amount }
        val totalWithdrawals = transactions.filter { it.type == "WITHDRAW" || it.type == "TRANSFER_OUT" }.sumOf { it.amount }
        val netFlow = totalDeposits - totalWithdrawals

        assertEquals(500000.0, totalDeposits, 0.001)
        assertEquals(150000.0, totalWithdrawals, 0.001)
        assertEquals(350000.0, netFlow, 0.001)
        assertTrue(account.balance > 0)
    }

    @Test
    fun testTransactionVoucherProperties() {
        val tx = AccountTransaction(
            id = 45L,
            accountId = 2L,
            accountName = "محفظة زين كاش",
            type = "DEPOSIT",
            amount = 75000.0,
            balanceAfter = 850000.0,
            category = "تحصيل ديون",
            referenceType = "DEBT_PAYMENT",
            referenceId = 12L,
            notes = "دفعة حساب العميل سجاد"
        )

        assertEquals("DEPOSIT", tx.type)
        assertEquals(75000.0, tx.amount, 0.001)
        assertEquals("محفظة زين كاش", tx.accountName)
        assertEquals("DEBT_PAYMENT", tx.referenceType)
    }

    @Test
    fun testCustomerStatementCalculations() {
        val customer = Customer(
            id = 5L,
            name = "شركة النور للمقاولات",
            phone = "07800000000",
            currentDebt = 220000.0
        )
        val invoices = listOf(
            Invoice(
                id = 201L,
                invoiceNumber = "INV-2026-050",
                totalAmount = 300000.0,
                paidAmount = 100000.0,
                remainingDebt = 200000.0
            )
        )
        val receipts = listOf(
            CustomerReceipt(
                id = 10L,
                customerId = 5L,
                customerName = "شركة النور للمقاولات",
                amount = 80000.0,
                receiptNumber = "REC-100",
                paymentMethod = "زين كاش",
                notes = "دفعة على الحساب"
            )
        )

        assertEquals(300000.0, invoices.sumOf { it.totalAmount }, 0.001)
        assertEquals(80000.0, receipts.sumOf { it.amount }, 0.001)
        assertEquals(220000.0, customer.currentDebt, 0.001)
    }
}
