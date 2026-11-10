package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Account
import com.example.data.model.AccountTransaction
import com.example.data.model.Customer
import com.example.data.model.CustomerReceipt
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import java.io.File
import java.io.FileOutputStream

object PdfInvoiceExportHelper {

    // Standard A4 dimensions in PostScript points (72 dpi)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2)

    /**
     * 1. Export Sales / POS Invoice as formatted PDF
     */
    fun exportInvoicePdf(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>,
        currencySymbol: String,
        isArabic: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            drawInvoicePage(canvas, invoice, items, currencySymbol, isArabic)
            document.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_invoices")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedInvNum = invoice.invoiceNumber.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val pdfFile = File(exportDir, "Invoice_${sanitizedInvNum}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { document.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * 2. Export Account Summary / Statement as formatted PDF
     */
    fun exportAccountSummaryPdf(
        context: Context,
        account: Account,
        transactions: List<AccountTransaction>,
        currencySymbol: String,
        isArabic: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            drawAccountSummaryPage(canvas, account, transactions, currencySymbol, isArabic)
            document.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_invoices")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedName = account.name.replace("[^a-zA-Z0-9\\u0600-\\u06FF_-]".toRegex(), "_")
            val pdfFile = File(exportDir, "AccountStatement_${sanitizedName}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { document.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * 3. Export Overview of All Accounts & Balances as formatted PDF
     */
    fun exportAllAccountsSummaryPdf(
        context: Context,
        accounts: List<Account>,
        totalBalance: Double,
        currencySymbol: String,
        isArabic: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            drawAllAccountsSummaryPage(canvas, accounts, totalBalance, currencySymbol, isArabic)
            document.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_invoices")
            if (!exportDir.exists()) exportDir.mkdirs()

            val pdfFile = File(exportDir, "AllAccountsSummary_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { document.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * 4. Export Individual Transaction Voucher (سند قبض / سند صرف / سند قيد مالي) as formatted PDF
     */
    fun exportTransactionVoucherPdf(
        context: Context,
        transaction: AccountTransaction,
        currencySymbol: String,
        isArabic: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            drawTransactionVoucherPage(canvas, transaction, currencySymbol, isArabic)
            document.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_invoices")
            if (!exportDir.exists()) exportDir.mkdirs()

            val pdfFile = File(exportDir, "Voucher_${transaction.type}_${transaction.id}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { document.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * 5. Export Customer Account Statement (كشف حساب عميل معتمد) as formatted PDF
     */
    fun exportCustomerAccountStatementPdf(
        context: Context,
        customer: Customer,
        invoices: List<Invoice>,
        receipts: List<CustomerReceipt>,
        currencySymbol: String,
        isArabic: Boolean = true
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        try {
            drawCustomerAccountStatementPage(canvas, customer, invoices, receipts, currencySymbol, isArabic)
            document.finishPage(page)

            val exportDir = File(context.cacheDir, "pdf_invoices")
            if (!exportDir.exists()) exportDir.mkdirs()

            val sanitizedName = customer.name.replace("[^a-zA-Z0-9\\u0600-\\u06FF_-]".toRegex(), "_")
            val pdfFile = File(exportDir, "CustomerStatement_${sanitizedName}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()

            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { document.close() } catch (_: Exception) {}
            return null
        }
    }

    /**
     * Share exported PDF via Android Share Sheet
     */
    fun sharePdf(context: Context, file: File, subjectTitle: String) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, subjectTitle)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, subjectTitle))
        } catch (e: Exception) {
            Toast.makeText(context, "تم حفظ ملف PDF: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Open exported PDF directly in a PDF reader
     */
    fun openPdf(context: Context, file: File) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            sharePdf(context, file, file.name)
        }
    }

    // ==========================================
    // Drawing Invoice Page Layout
    // ==========================================
    private fun drawInvoicePage(
        canvas: Canvas,
        invoice: Invoice,
        items: List<InvoiceItem>,
        currencySymbol: String,
        isArabic: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner Background (Dark Slate)
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        // Gold brand stripe
        paint.color = Color.rgb(212, 175, 55) // Gold
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat(), 114f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEDGER • ليدجر", MARGIN, 45f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(253, 224, 71) // Light Gold
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            if (isArabic) "النظام المحاسبي الذكي والمتكامل - فاتورة بيع معتمدة" else "Smart Accounting System - Official Invoice",
            MARGIN,
            65f,
            paint
        )

        // Right side header info
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val invTitle = if (isArabic) "فاتورة مبيعات #${invoice.invoiceNumber}" else "INVOICE #${invoice.invoiceNumber}"
        val invTitleWidth = paint.measureText(invTitle)
        canvas.drawText(invTitle, PAGE_WIDTH - MARGIN - invTitleWidth, 45f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = LanguageManager.formatDate(invoice.createdAt, isArabic)
        val dateWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dateWidth, 65f, paint)

        var curY = 135f

        // 2. Invoice Meta Box
        paint.color = Color.rgb(248, 250, 252) // Slate 50
        val metaRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 65f)
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(226, 232, 240) // Slate 200
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Meta Text
        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139) // Slate 500
        canvas.drawText(if (isArabic) "العميل:" else "Customer:", MARGIN + 12f, curY + 22f, paint)
        canvas.drawText(if (isArabic) "الهاتف:" else "Phone:", MARGIN + 12f, curY + 45f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.customerName.ifBlank { if (isArabic) "عميل نقدي" else "Cash Customer" }, MARGIN + 65f, curY + 22f, paint)
        canvas.drawText(invoice.customerPhone.ifBlank { "-" }, MARGIN + 65f, curY + 45f, paint)

        val midX = MARGIN + (CONTENT_WIDTH / 2f)
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isArabic) "المسؤول:" else "Cashier:", midX, curY + 22f, paint)
        canvas.drawText(if (isArabic) "حالة الدفع:" else "Status:", midX, curY + 45f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(invoice.createdBy, midX + 55f, curY + 22f, paint)

        val statusText = if (invoice.remainingDebt > 0) {
            if (isArabic) "آجل / متبقي دين" else "Partial / Debt"
        } else {
            if (isArabic) "مدفوع بالكامل ✓" else "Fully Paid ✓"
        }
        paint.color = if (invoice.remainingDebt > 0) Color.rgb(225, 29, 72) else Color.rgb(5, 150, 105)
        canvas.drawText(statusText, midX + 55f, curY + 45f, paint)

        curY += 85f

        // 3. Items Table Header
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        val tableHeadRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 24f)
        canvas.drawRoundRect(tableHeadRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val col1X = MARGIN + 12f // Item
        val col2X = MARGIN + 260f // Qty
        val col3X = MARGIN + 340f // Price
        val col4X = PAGE_WIDTH - MARGIN - 70f // Total

        canvas.drawText(if (isArabic) "المادة / الصنف" else "Item / Description", col1X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "الكمية" else "Qty", col2X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "السعر" else "Price", col3X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "المجموع" else "Total", col4X, curY + 16f, paint)

        curY += 28f

        // Table Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f

        items.forEachIndexed { index, item ->
            // Alternating row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, curY - 4f, PAGE_WIDTH - MARGIN, curY + 18f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            val nameText = if (item.productName.length > 35) item.productName.take(32) + "..." else item.productName
            canvas.drawText(nameText, col1X, curY + 11f, paint)
            canvas.drawText("${item.quantity}", col2X, curY + 11f, paint)
            canvas.drawText(formatNum(item.unitPrice), col3X, curY + 11f, paint)
            canvas.drawText(formatNum(item.totalPrice), col4X, curY + 11f, paint)

            // Divider line
            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN, curY + 18f, PAGE_WIDTH - MARGIN, curY + 18f, paint)

            curY += 22f
        }

        curY += 10f

        // 4. Financial Summary Card (Right aligned)
        val summaryBoxWidth = 220f
        val summaryBoxX = PAGE_WIDTH - MARGIN - summaryBoxWidth
        val summaryBoxHeight = 110f

        paint.color = Color.rgb(248, 250, 252)
        val summaryRect = RectF(summaryBoxX, curY, PAGE_WIDTH - MARGIN, curY + summaryBoxHeight)
        canvas.drawRoundRect(summaryRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(summaryRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var sY = curY + 18f
        drawSummaryRow(canvas, paint, summaryBoxX + 10f, summaryBoxX + summaryBoxWidth - 10f, sY, if (isArabic) "المجموع الفرعي:" else "Subtotal:", formatMoney(invoice.totalAmount + invoice.discountAmount - invoice.taxAmount, currencySymbol))
        sY += 18f

        if (invoice.discountAmount > 0) {
            drawSummaryRow(canvas, paint, summaryBoxX + 10f, summaryBoxX + summaryBoxWidth - 10f, sY, if (isArabic) "الخصم:" else "Discount:", "-${formatMoney(invoice.discountAmount, currencySymbol)}", Color.rgb(225, 29, 72))
            sY += 18f
        }

        if (invoice.taxAmount > 0) {
            drawSummaryRow(canvas, paint, summaryBoxX + 10f, summaryBoxX + summaryBoxWidth - 10f, sY, if (isArabic) "الضريبة (${invoice.taxRate}%):" else "Tax (${invoice.taxRate}%):", formatMoney(invoice.taxAmount, currencySymbol))
            sY += 18f
        }

        // Total Row
        paint.color = Color.rgb(15, 23, 42)
        paint.strokeWidth = 1f
        canvas.drawLine(summaryBoxX + 8f, sY - 2f, summaryBoxX + summaryBoxWidth - 8f, sY - 2f, paint)

        sY += 12f
        drawSummaryRow(canvas, paint, summaryBoxX + 10f, summaryBoxX + summaryBoxWidth - 10f, sY, if (isArabic) "الإجمالي النهائي:" else "Grand Total:", formatMoney(invoice.totalAmount, currencySymbol), Color.rgb(2, 132, 199), isBold = true)

        sY += 18f
        drawSummaryRow(canvas, paint, summaryBoxX + 10f, summaryBoxX + summaryBoxWidth - 10f, sY, if (isArabic) "المبلغ المدفوع:" else "Paid Amount:", formatMoney(invoice.paidAmount, currencySymbol), Color.rgb(5, 150, 105))

        if (invoice.remainingDebt > 0) {
            sY += 18f
            drawSummaryRow(canvas, paint, summaryBoxX + 10f, summaryBoxX + summaryBoxWidth - 10f, sY, if (isArabic) "المتبقي (دين):" else "Remaining Debt:", formatMoney(invoice.remainingDebt, currencySymbol), Color.rgb(225, 29, 72), isBold = true)
        }

        // 5. Signature & Stamp Box on Left
        val signRect = RectF(MARGIN, curY, MARGIN + 220f, curY + 80f)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(signRect, 8f, 8f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(signRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(if (isArabic) "توقيع المحاسب / الختم المعتمد:" else "Authorized Signature & Stamp:", MARGIN + 10f, curY + 18f, paint)

        // Dotted signature line
        paint.strokeWidth = 1f
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(MARGIN + 15f, curY + 65f, MARGIN + 205f, curY + 65f, paint)

        // 6. Footer
        val footerY = PAGE_HEIGHT - 45f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, footerY - 15f, PAGE_WIDTH - MARGIN, footerY - 15f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = if (isArabic) {
            "شكراً لتعاملكم معنا • نظام ليدجر Ledger المحاسبي • هاتف الدعم الفني: 07811996908"
        } else {
            "Thank you for your business • Powered by Ledger Smart Accounting • Support: 07811996908"
        }
        val fWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (PAGE_WIDTH - fWidth) / 2f, footerY, paint)
    }

    // ==========================================
    // Drawing Account Summary / Statement Layout
    // ==========================================
    private fun drawAccountSummaryPage(
        canvas: Canvas,
        account: Account,
        transactions: List<AccountTransaction>,
        currencySymbol: String,
        isArabic: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner Background (Dark Slate)
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        paint.color = Color.rgb(5, 150, 105) // Emerald Stripe for Accounts
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat(), 114f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEDGER • ليدجر", MARGIN, 45f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(167, 243, 208) // Light Emerald
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            if (isArabic) "كشف حساب مالي معتمد وسجل حركة الصناديق" else "Official Statement of Account & Cash Flow",
            MARGIN,
            65f,
            paint
        )

        // Right side header info
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val docTitle = if (isArabic) "كشف حساب: ${account.name}" else "Account: ${account.name}"
        val docWidth = paint.measureText(docTitle)
        canvas.drawText(docTitle, PAGE_WIDTH - MARGIN - docWidth, 45f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = LanguageManager.formatDate(System.currentTimeMillis(), isArabic)
        val dateWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dateWidth, 65f, paint)

        var curY = 135f

        // 2. Account Summary Header Card
        paint.color = Color.rgb(248, 250, 252)
        val metaRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 70f)
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(209, 250, 229) // Emerald tint border
        canvas.drawRoundRect(metaRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(if (isArabic) "اسم الحساب:" else "Account Name:", MARGIN + 12f, curY + 22f, paint)
        canvas.drawText(if (isArabic) "نوع الحساب:" else "Type / Code:", MARGIN + 12f, curY + 48f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(account.name, MARGIN + 80f, curY + 22f, paint)

        val typeLabel = when (account.type) {
            "CASH" -> if (isArabic) "خزينة نقدية" else "Cash Treasury"
            "BANK" -> if (isArabic) "حساب مصرفي" else "Bank Account"
            "WALLET" -> if (isArabic) "محفظة رقمية" else "Digital Wallet"
            else -> account.type
        }
        val codeInfo = if (account.code.isNotBlank()) " [${account.code}]" else ""
        canvas.drawText("$typeLabel$codeInfo", MARGIN + 80f, curY + 48f, paint)

        // Current Balance display in green
        val rightX = PAGE_WIDTH - MARGIN - 180f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isArabic) "الرصيد المتاح الحالي:" else "Current Balance:", rightX, curY + 22f, paint)

        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (account.balance >= 0) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
        canvas.drawText(formatMoney(account.balance, currencySymbol), rightX, curY + 48f, paint)

        curY += 90f

        // 3. Transactions Metrics Strip (Deposits vs Withdrawals)
        val totalDeposits = transactions.filter { it.type == "DEPOSIT" || it.type == "TRANSFER_IN" }.sumOf { it.amount }
        val totalWithdrawals = transactions.filter { it.type == "WITHDRAW" || it.type == "TRANSFER_OUT" }.sumOf { it.amount }
        val netFlow = totalDeposits - totalWithdrawals

        paint.color = Color.rgb(241, 245, 249)
        val flowRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 36f)
        canvas.drawRoundRect(flowRect, 6f, 6f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(if (isArabic) "إجمالي الإيداعات (+):" else "Total Inflows (+):", MARGIN + 12f, curY + 22f, paint)

        paint.color = Color.rgb(5, 150, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatMoney(totalDeposits, currencySymbol), MARGIN + 115f, curY + 22f, paint)

        val midFlowX = MARGIN + 220f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isArabic) "إجمالي السحوبات (-):" else "Total Outflows (-):", midFlowX, curY + 22f, paint)

        paint.color = Color.rgb(225, 29, 72)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatMoney(totalWithdrawals, currencySymbol), midFlowX + 115f, curY + 22f, paint)

        val rightFlowX = PAGE_WIDTH - MARGIN - 130f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isArabic) "صافي التدفق:" else "Net Flow:", rightFlowX, curY + 22f, paint)

        paint.color = if (netFlow >= 0) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(formatMoney(netFlow, currencySymbol), rightFlowX + 55f, curY + 22f, paint)

        curY += 50f

        // 4. Transactions Table Header
        paint.color = Color.rgb(15, 23, 42)
        val tableHeadRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 24f)
        canvas.drawRoundRect(tableHeadRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val tCol1X = MARGIN + 10f // Date
        val tCol2X = MARGIN + 100f // Type
        val tCol3X = MARGIN + 175f // Category / Notes
        val tCol4X = MARGIN + 360f // Amount
        val tCol5X = PAGE_WIDTH - MARGIN - 75f // Balance After

        canvas.drawText(if (isArabic) "التاريخ" else "Date", tCol1X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "نوع الحركة" else "Type", tCol2X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "البيان / التصنيف" else "Description / Category", tCol3X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "المبلغ" else "Amount", tCol4X, curY + 16f, paint)
        canvas.drawText(if (isArabic) "الرصيد بعده" else "Balance", tCol5X, curY + 16f, paint)

        curY += 28f

        // Rows
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f

        val displayTx = transactions.take(20)
        if (displayTx.isEmpty()) {
            paint.color = Color.rgb(148, 163, 184)
            canvas.drawText(if (isArabic) "لا توجد قيود مسجلة لهذا الحساب حالياً" else "No transactions recorded for this account", MARGIN + 20f, curY + 15f, paint)
            curY += 25f
        } else {
            displayTx.forEachIndexed { index, tx ->
                val isDeposit = tx.type == "DEPOSIT" || tx.type == "TRANSFER_IN"
                if (index % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(MARGIN, curY - 4f, PAGE_WIDTH - MARGIN, curY + 18f, paint)
                }

                paint.color = Color.rgb(71, 85, 105)
                canvas.drawText(LanguageManager.formatDateShort(tx.createdAt, isArabic), tCol1X, curY + 11f, paint)

                paint.color = if (isDeposit) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
                val typeName = when (tx.type) {
                    "DEPOSIT" -> if (isArabic) "إيداع نقدي" else "Deposit"
                    "WITHDRAW" -> if (isArabic) "سحب نقدي" else "Withdrawal"
                    "TRANSFER_IN" -> if (isArabic) "تحويل وارد" else "Transfer In"
                    "TRANSFER_OUT" -> if (isArabic) "تحويل صادر" else "Transfer Out"
                    else -> tx.type
                }
                canvas.drawText(typeName, tCol2X, curY + 11f, paint)

                paint.color = Color.rgb(15, 23, 42)
                val desc = "${tx.category} - ${tx.notes}".take(30)
                canvas.drawText(desc, tCol3X, curY + 11f, paint)

                paint.color = if (isDeposit) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val sign = if (isDeposit) "+" else "-"
                canvas.drawText("$sign${formatNum(tx.amount)}", tCol4X, curY + 11f, paint)

                paint.color = Color.rgb(15, 23, 42)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(formatNum(tx.balanceAfter), tCol5X, curY + 11f, paint)

                paint.color = Color.rgb(241, 245, 249)
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, curY + 18f, PAGE_WIDTH - MARGIN, curY + 18f, paint)

                curY += 21f
            }
        }

        // Footer
        val footerY = PAGE_HEIGHT - 45f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, footerY - 15f, PAGE_WIDTH - MARGIN, footerY - 15f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = if (isArabic) {
            "تم إصدار هذا الكشف المالي آلياً عبر نظام ليدجر Ledger • هاتف الدعم الفني: 07811996908"
        } else {
            "Generated automatically by Ledger Financial Accounting • Support: 07811996908"
        }
        val fWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (PAGE_WIDTH - fWidth) / 2f, footerY, paint)
    }

    // ==========================================
    // Drawing All Accounts Overview Layout
    // ==========================================
    private fun drawAllAccountsSummaryPage(
        canvas: Canvas,
        accounts: List<Account>,
        totalBalance: Double,
        currencySymbol: String,
        isArabic: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        paint.color = Color.rgb(212, 175, 55) // Gold
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat(), 114f, paint)

        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEDGER • ليدجر", MARGIN, 45f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(253, 224, 71)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            if (isArabic) "تقرير الأرصدة والسيولة النقدية الشامل لجميع الحسابات" else "Comprehensive Balances & Liquidity Summary",
            MARGIN,
            65f,
            paint
        )

        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val docTitle = if (isArabic) "تقرير الصناديق والخزائن" else "TREASURY REPORT"
        val docWidth = paint.measureText(docTitle)
        canvas.drawText(docTitle, PAGE_WIDTH - MARGIN - docWidth, 45f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = LanguageManager.formatDate(System.currentTimeMillis(), isArabic)
        val dateWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dateWidth, 65f, paint)

        var curY = 135f

        // Total Treasury Card
        paint.color = Color.rgb(248, 250, 252)
        val totalRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 65f)
        canvas.drawRoundRect(totalRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(212, 175, 55)
        canvas.drawRoundRect(totalRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 11f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(if (isArabic) "إجمالي السيولة النقدية والمصرفية المتوفرة في جميع الحسابات:" else "Total Available Liquid Balance across All Accounts:", MARGIN + 14f, curY + 24f, paint)

        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(5, 150, 105)
        canvas.drawText(formatMoney(totalBalance, currencySymbol), MARGIN + 14f, curY + 50f, paint)

        curY += 85f

        // Table Header
        paint.color = Color.rgb(15, 23, 42)
        val tHeadRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 24f)
        canvas.drawRoundRect(tHeadRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val col1 = MARGIN + 12f
        val col2 = MARGIN + 180f
        val col3 = MARGIN + 280f
        val col4 = PAGE_WIDTH - MARGIN - 80f

        canvas.drawText(if (isArabic) "اسم الحساب / الخزينة" else "Account Name", col1, curY + 16f, paint)
        canvas.drawText(if (isArabic) "الرمز / الكود" else "Code", col2, curY + 16f, paint)
        canvas.drawText(if (isArabic) "النوع" else "Type", col3, curY + 16f, paint)
        canvas.drawText(if (isArabic) "الرصيد المتاح" else "Balance", col4, curY + 16f, paint)

        curY += 28f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f

        accounts.forEachIndexed { idx, acc ->
            if (idx % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas.drawRect(MARGIN, curY - 4f, PAGE_WIDTH - MARGIN, curY + 20f, paint)
            }

            paint.color = Color.rgb(15, 23, 42)
            canvas.drawText(acc.name, col1, curY + 12f, paint)

            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText(if (acc.code.isNotBlank()) acc.code else "-", col2, curY + 12f, paint)

            val typeDesc = when (acc.type) {
                "CASH" -> if (isArabic) "خزينة نقدية" else "Cash"
                "BANK" -> if (isArabic) "حساب بنكي" else "Bank"
                "WALLET" -> if (isArabic) "محفظة رقمية" else "Wallet"
                else -> acc.type
            }
            canvas.drawText(typeDesc, col3, curY + 12f, paint)

            paint.color = if (acc.balance >= 0) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(formatMoney(acc.balance, currencySymbol), col4, curY + 12f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            paint.color = Color.rgb(241, 245, 249)
            paint.strokeWidth = 0.5f
            canvas.drawLine(MARGIN, curY + 20f, PAGE_WIDTH - MARGIN, curY + 20f, paint)

            curY += 24f
        }

        // Footer
        val footerY = PAGE_HEIGHT - 45f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, footerY - 15f, PAGE_WIDTH - MARGIN, footerY - 15f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = if (isArabic) {
            "تقرير رسمي معتمد • نظام ليدجر Ledger المحاسبي • هاتف الدعم الفني: 07811996908"
        } else {
            "Official Report • Ledger Smart Accounting System • Support: 07811996908"
        }
        val fWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (PAGE_WIDTH - fWidth) / 2f, footerY, paint)
    }

    // ==========================================
    // Drawing Transaction Financial Voucher Layout
    // ==========================================
    private fun drawTransactionVoucherPage(
        canvas: Canvas,
        tx: AccountTransaction,
        currencySymbol: String,
        isArabic: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val isDeposit = tx.type == "DEPOSIT" || tx.type == "TRANSFER_IN"

        // 1. Header Banner Background (Dark Slate)
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        // Accent Stripe based on transaction nature
        paint.color = if (isDeposit) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat(), 114f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEDGER • ليدجر", MARGIN, 45f, paint)

        paint.textSize = 10f
        paint.color = if (isDeposit) Color.rgb(167, 243, 208) else Color.rgb(254, 205, 211)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val voucherSub = if (isArabic) "سند مالي رسمي معتمد • توثيق قيود الحسابات والصناديق" else "Official Financial Voucher • Account Ledger"
        canvas.drawText(voucherSub, MARGIN, 65f, paint)

        // Right side header info
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val vTitle = when (tx.type) {
            "DEPOSIT" -> if (isArabic) "سند قبض نقدي #TX-${tx.id}" else "RECEIPT VOUCHER #TX-${tx.id}"
            "WITHDRAW" -> if (isArabic) "سند صرف نقدي #TX-${tx.id}" else "PAYMENT VOUCHER #TX-${tx.id}"
            "TRANSFER_IN" -> if (isArabic) "سند تحويل وارد #TX-${tx.id}" else "TRANSFER IN #TX-${tx.id}"
            "TRANSFER_OUT" -> if (isArabic) "سند تحويل صادر #TX-${tx.id}" else "TRANSFER OUT #TX-${tx.id}"
            else -> if (isArabic) "سند قيد مالي #TX-${tx.id}" else "JOURNAL VOUCHER #TX-${tx.id}"
        }
        val vWidth = paint.measureText(vTitle)
        canvas.drawText(vTitle, PAGE_WIDTH - MARGIN - vWidth, 45f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateStr = LanguageManager.formatDate(tx.createdAt, isArabic)
        val dateWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dateWidth, 65f, paint)

        var curY = 135f

        // 2. Amount Highlight Banner Box
        val amountBoxHeight = 72f
        val amountRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + amountBoxHeight)
        paint.color = if (isDeposit) Color.rgb(240, 253, 244) else Color.rgb(255, 241, 242)
        canvas.drawRoundRect(amountRect, 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        paint.color = if (isDeposit) Color.rgb(187, 247, 208) else Color.rgb(254, 205, 211)
        canvas.drawRoundRect(amountRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139)
        val amountLabel = if (isDeposit) {
            if (isArabic) "المبلغ المقبوض في الحساب:" else "Amount Received:"
        } else {
            if (isArabic) "المبلغ المصروف من الحساب:" else "Amount Paid Out:"
        }
        canvas.drawText(amountLabel, MARGIN + 16f, curY + 24f, paint)

        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (isDeposit) Color.rgb(5, 150, 105) else Color.rgb(225, 29, 72)
        val sign = if (isDeposit) "+" else "-"
        val formattedAmt = "$sign${formatMoney(tx.amount, currencySymbol)}"
        canvas.drawText(formattedAmt, MARGIN + 16f, curY + 54f, paint)

        // Type badge on right side of amount box
        val typeBadgeText = when (tx.type) {
            "DEPOSIT" -> if (isArabic) "إيداع نقدي ✓" else "Cash Deposit ✓"
            "WITHDRAW" -> if (isArabic) "سحب نقدي ⚠" else "Cash Withdrawal ⚠"
            "TRANSFER_IN" -> if (isArabic) "تحويل مالي وارد" else "Transfer In"
            "TRANSFER_OUT" -> if (isArabic) "تحويل مالي صادر" else "Transfer Out"
            else -> tx.type
        }
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val tbWidth = paint.measureText(typeBadgeText)
        canvas.drawText(typeBadgeText, PAGE_WIDTH - MARGIN - tbWidth - 16f, curY + 44f, paint)

        curY += 92f

        // 3. Voucher Detailed Specifications Card
        paint.color = Color.rgb(248, 250, 252)
        val specRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 130f)
        canvas.drawRoundRect(specRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(specRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Rows in Specs Card
        var sY = curY + 26f
        val leftColX = MARGIN + 16f
        val rightColX = MARGIN + (CONTENT_WIDTH / 2f)

        // Row 1: Account & Category
        drawVoucherDetailField(canvas, paint, leftColX, sY, if (isArabic) "اسم الحساب / الصندوق:" else "Account Name:", tx.accountName)
        drawVoucherDetailField(canvas, paint, rightColX, sY, if (isArabic) "التصنيف / التبويب:" else "Category:", tx.category)

        sY += 34f
        // Row 2: Reference Type & Balance After
        val refDesc = "${tx.referenceType} #${tx.referenceId ?: "-"}"
        drawVoucherDetailField(canvas, paint, leftColX, sY, if (isArabic) "المرجع المحاسبي:" else "Reference:", refDesc)
        drawVoucherDetailField(canvas, paint, rightColX, sY, if (isArabic) "الرصيد بعد الحركة:" else "Balance After:", formatMoney(tx.balanceAfter, currencySymbol), isAmount = true)

        sY += 34f
        // Row 3: Description / Notes
        val notesText = if (tx.notes.isNotBlank()) tx.notes else if (isArabic) "قيد مالي نظامي معتمد" else "Standard approved entry"
        drawVoucherDetailField(canvas, paint, leftColX, sY, if (isArabic) "البيان وملاحظات القيد:" else "Notes / Description:", notesText)

        curY += 150f

        // 4. Verification & Legal Notice Box
        val noticeRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 45f)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(noticeRect, 6f, 6f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val notice1 = if (isArabic) "إقرار: يعتبر هذا السند وثيقة مالية معتمدة تم ترحيلها إلى دفتر الأستاذ العام وسجل الحركات المحاسبية." else "Confirmation: This voucher serves as official accounting documentation posted to the general ledger."
        canvas.drawText(notice1, MARGIN + 12f, curY + 20f, paint)
        val notice2 = if (isArabic) "رقم السند المرجعي مشفر وموثق برمجياً في قاعدة بيانات النظام." else "System Reference ID cryptographically stored in the database."
        canvas.drawText(notice2, MARGIN + 12f, curY + 34f, paint)

        curY += 65f

        // 5. Official Signature & Stamp Section (3 Boxes)
        val sigBoxWidth = (CONTENT_WIDTH - 20f) / 3f

        // Box 1: Cashier
        drawSignatureBox(canvas, paint, MARGIN, curY, sigBoxWidth, 80f, if (isArabic) "توقيع أمين الصندوق" else "Accountant / Cashier")
        // Box 2: Beneficiary
        drawSignatureBox(canvas, paint, MARGIN + sigBoxWidth + 10f, curY, sigBoxWidth, 80f, if (isArabic) "توقيع المستلم / الدافع" else "Beneficiary / Depositor")
        // Box 3: Management Stamp
        drawSignatureBox(canvas, paint, MARGIN + (sigBoxWidth * 2f) + 20f, curY, sigBoxWidth, 80f, if (isArabic) "ختم الإدارة والاعتماد" else "Official Management Stamp")

        // Footer
        val footerY = PAGE_HEIGHT - 45f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, footerY - 15f, PAGE_WIDTH - MARGIN, footerY - 15f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val fText = if (isArabic) {
            "تم إصدار هذا السند المالي آلياً عبر نظام ليدجر Ledger المحاسبي • هاتف الدعم الفني: 07811996908"
        } else {
            "Generated automatically by Ledger Accounting System • Support: 07811996908"
        }
        val fw = paint.measureText(fText)
        canvas.drawText(fText, (PAGE_WIDTH - fw) / 2f, footerY, paint)
    }

    // ==========================================
    // Drawing Customer Account Statement Layout
    // ==========================================
    private fun drawCustomerAccountStatementPage(
        canvas: Canvas,
        customer: Customer,
        invoices: List<Invoice>,
        receipts: List<CustomerReceipt>,
        currencySymbol: String,
        isArabic: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Banner
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 110f, paint)

        paint.color = Color.rgb(2, 132, 199) // Sky Blue
        canvas.drawRect(0f, 110f, PAGE_WIDTH.toFloat(), 114f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("LEDGER • ليدجر", MARGIN, 45f, paint)

        paint.textSize = 10f
        paint.color = Color.rgb(186, 230, 253)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            if (isArabic) "كشف حساب عميل مالي معتمد - سجل الفواتير والمقبوضات" else "Official Customer Statement - Invoices & Receipts",
            MARGIN,
            65f,
            paint
        )

        // Right side
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleText = if (isArabic) "كشف حساب: ${customer.name}" else "Statement: ${customer.name}"
        val tWidth = paint.measureText(titleText)
        canvas.drawText(titleText, PAGE_WIDTH - MARGIN - tWidth, 45f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(203, 213, 225)
        val dateStr = LanguageManager.formatDate(System.currentTimeMillis(), isArabic)
        val dWidth = paint.measureText(dateStr)
        canvas.drawText(dateStr, PAGE_WIDTH - MARGIN - dWidth, 65f, paint)

        var curY = 135f

        // 2. Customer Summary Box
        paint.color = Color.rgb(248, 250, 252)
        val custRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 65f)
        canvas.drawRoundRect(custRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRoundRect(custRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 10f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(if (isArabic) "العميل:" else "Customer:", MARGIN + 12f, curY + 22f, paint)
        canvas.drawText(if (isArabic) "الهاتف:" else "Phone:", MARGIN + 12f, curY + 45f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(customer.name, MARGIN + 60f, curY + 22f, paint)
        canvas.drawText(customer.phone.ifBlank { "-" }, MARGIN + 60f, curY + 45f, paint)

        // Current Debt highlight
        val rightX = PAGE_WIDTH - MARGIN - 180f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(if (isArabic) "إجمالي الدين الحالي المستحق:" else "Current Outstanding Debt:", rightX, curY + 22f, paint)

        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (customer.currentDebt > 0) Color.rgb(225, 29, 72) else Color.rgb(5, 150, 105)
        canvas.drawText(formatMoney(customer.currentDebt, currencySymbol), rightX, curY + 48f, paint)

        curY += 80f

        // 3. Invoices Table Header
        paint.color = Color.rgb(15, 23, 42)
        val invHeadRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 22f)
        canvas.drawRoundRect(invHeadRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val iCol1 = MARGIN + 10f // Inv #
        val iCol2 = MARGIN + 120f // Date
        val iCol3 = MARGIN + 230f // Total
        val iCol4 = MARGIN + 350f // Paid
        val iCol5 = PAGE_WIDTH - MARGIN - 80f // Remaining

        canvas.drawText(if (isArabic) "رقم الفاتورة" else "Invoice #", iCol1, curY + 15f, paint)
        canvas.drawText(if (isArabic) "التاريخ" else "Date", iCol2, curY + 15f, paint)
        canvas.drawText(if (isArabic) "المبلغ الكلي" else "Total", iCol3, curY + 15f, paint)
        canvas.drawText(if (isArabic) "المدفوع" else "Paid", iCol4, curY + 15f, paint)
        canvas.drawText(if (isArabic) "المتبقي (دين)" else "Debt", iCol5, curY + 15f, paint)

        curY += 26f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f

        val displayInvoices = invoices.take(6)
        if (displayInvoices.isEmpty()) {
            paint.color = Color.rgb(148, 163, 184)
            canvas.drawText(if (isArabic) "لا توجد فواتير مبيعات مسجلة لهذا العميل" else "No invoices recorded for this customer", MARGIN + 15f, curY + 14f, paint)
            curY += 22f
        } else {
            displayInvoices.forEachIndexed { idx, inv ->
                if (idx % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(MARGIN, curY - 4f, PAGE_WIDTH - MARGIN, curY + 18f, paint)
                }

                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText("#${inv.invoiceNumber}", iCol1, curY + 11f, paint)
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText(LanguageManager.formatDateShort(inv.createdAt, isArabic), iCol2, curY + 11f, paint)
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(formatNum(inv.totalAmount), iCol3, curY + 11f, paint)
                paint.color = Color.rgb(5, 150, 105)
                canvas.drawText(formatNum(inv.paidAmount), iCol4, curY + 11f, paint)
                paint.color = if (inv.remainingDebt > 0) Color.rgb(225, 29, 72) else Color.rgb(100, 116, 139)
                canvas.drawText(formatNum(inv.remainingDebt), iCol5, curY + 11f, paint)

                paint.color = Color.rgb(241, 245, 249)
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, curY + 18f, PAGE_WIDTH - MARGIN, curY + 18f, paint)

                curY += 20f
            }
        }

        curY += 14f

        // 4. Customer Receipts (Payments) Table Header
        paint.color = Color.rgb(5, 150, 105) // Emerald Head
        val recHeadRect = RectF(MARGIN, curY, PAGE_WIDTH - MARGIN, curY + 22f)
        canvas.drawRoundRect(recHeadRect, 4f, 4f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val rCol1 = MARGIN + 10f // Receipt #
        val rCol2 = MARGIN + 120f // Date
        val rCol3 = MARGIN + 230f // Method
        val rCol4 = MARGIN + 350f // Amount
        val rCol5 = PAGE_WIDTH - MARGIN - 120f // Notes

        canvas.drawText(if (isArabic) "رقم سند القبض" else "Receipt #", rCol1, curY + 15f, paint)
        canvas.drawText(if (isArabic) "التاريخ" else "Date", rCol2, curY + 15f, paint)
        canvas.drawText(if (isArabic) "طريقة القبض" else "Method", rCol3, curY + 15f, paint)
        canvas.drawText(if (isArabic) "المبلغ المقبوض" else "Paid Amount", rCol4, curY + 15f, paint)
        canvas.drawText(if (isArabic) "البيان / ملاحظات" else "Notes", rCol5, curY + 15f, paint)

        curY += 26f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f

        val displayReceipts = receipts.take(5)
        if (displayReceipts.isEmpty()) {
            paint.color = Color.rgb(148, 163, 184)
            canvas.drawText(if (isArabic) "لا توجد سندات قبض مسجلة" else "No payment receipts recorded", MARGIN + 15f, curY + 14f, paint)
            curY += 22f
        } else {
            displayReceipts.forEachIndexed { idx, rec ->
                if (idx % 2 == 1) {
                    paint.color = Color.rgb(248, 250, 252)
                    canvas.drawRect(MARGIN, curY - 4f, PAGE_WIDTH - MARGIN, curY + 18f, paint)
                }

                paint.color = Color.rgb(5, 150, 105)
                canvas.drawText("#${rec.receiptNumber}", rCol1, curY + 11f, paint)
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText(LanguageManager.formatDateShort(rec.createdAt, isArabic), rCol2, curY + 11f, paint)
                paint.color = Color.rgb(15, 23, 42)
                canvas.drawText(rec.paymentMethod, rCol3, curY + 11f, paint)
                paint.color = Color.rgb(5, 150, 105)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(formatNum(rec.amount), rCol4, curY + 11f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText(rec.notes.take(25), rCol5, curY + 11f, paint)

                paint.color = Color.rgb(241, 245, 249)
                paint.strokeWidth = 0.5f
                canvas.drawLine(MARGIN, curY + 18f, PAGE_WIDTH - MARGIN, curY + 18f, paint)

                curY += 20f
            }
        }

        // Footer
        val footerY = PAGE_HEIGHT - 45f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, footerY - 15f, PAGE_WIDTH - MARGIN, footerY - 15f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val footerText = if (isArabic) {
            "كشف حساب معتمد • نظام ليدجر Ledger المحاسبي • هاتف الدعم الفني: 07811996908"
        } else {
            "Certified Account Statement • Ledger Accounting • Support: 07811996908"
        }
        val fw = paint.measureText(footerText)
        canvas.drawText(footerText, (PAGE_WIDTH - fw) / 2f, footerY, paint)
    }

    private fun drawVoucherDetailField(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        label: String,
        value: String,
        isAmount: Boolean = false
    ) {
        paint.textSize = 9.5f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(label, x, y, paint)

        paint.textSize = if (isAmount) 12f else 10.5f
        paint.color = if (isAmount) Color.rgb(5, 150, 105) else Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, x, y + 16f, paint)
    }

    private fun drawSignatureBox(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String
    ) {
        val rect = RectF(x, y, x + width, y + height)
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawRoundRect(rect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = 8.5f
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val tW = paint.measureText(title)
        canvas.drawText(title, x + ((width - tW) / 2f), y + 18f, paint)

        // Dotted sign line
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(x + 10f, y + height - 16f, x + width - 10f, y + height - 16f, paint)
    }

    // ==========================================
    // Formatting Helpers
    // ==========================================
    private fun drawSummaryRow(
        canvas: Canvas,
        paint: Paint,
        leftX: Float,
        rightX: Float,
        y: Float,
        label: String,
        value: String,
        valueColor: Int = Color.rgb(15, 23, 42),
        isBold: Boolean = false
    ) {
        paint.color = Color.rgb(100, 116, 139)
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText(label, leftX, y, paint)

        paint.color = valueColor
        paint.typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
        val valWidth = paint.measureText(value)
        canvas.drawText(value, rightX - valWidth, y, paint)
    }

    private fun formatMoney(amount: Double, currencySymbol: String): String {
        return "${formatNum(amount)} $currencySymbol"
    }

    private fun formatNum(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            String.format("%,d", amount.toLong())
        } else {
            String.format("%,.2f", amount)
        }
    }
}
