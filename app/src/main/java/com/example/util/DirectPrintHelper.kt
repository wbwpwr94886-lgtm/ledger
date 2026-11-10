package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem

object DirectPrintHelper {

    fun printInvoice(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>,
        currencySymbol: String
    ) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("فاتورة_${invoice.invoiceNumber}")
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager?.print("فاتورة_${invoice.invoiceNumber}", printAdapter, attributes)
            }
        }

        val htmlContent = generateInvoiceHtml(invoice, items, currencySymbol)
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }

    fun shareInvoiceText(
        context: Context,
        invoice: Invoice,
        items: List<InvoiceItem>,
        currencySymbol: String
    ) {
        val itemsList = items.joinToString("\n") {
            "  ${it.productName} | الكمية: ${it.quantity} | السعر: ${it.unitPrice} | المجموع: ${it.totalPrice} $currencySymbol"
        }

        val printText = """
            ====================================
               نظام المحاسبة الشامل - فاتورة بيع
            ====================================
            رقم الفاتورة: #${invoice.invoiceNumber}
            التاريخ: ${LanguageManager.formatDate(invoice.createdAt, true)}
            العميل: ${invoice.customerName.ifBlank { "عميل نقدي" }}
            هاتف: ${invoice.customerPhone.ifBlank { "-" }}
            الموظف: ${invoice.createdBy}
            ------------------------------------
            المواد:
            $itemsList
            ------------------------------------
            الإجمالي: ${invoice.totalAmount} $currencySymbol
            الضريبة: ${invoice.taxAmount} $currencySymbol (${invoice.taxRate}%)
            المدفوع: ${invoice.paidAmount} $currencySymbol
            المتبقي (دين): ${invoice.remainingDebt} $currencySymbol
            ====================================
            مطور النظام: أحمد فرحان السلماني
            هاتف المساعدة: 07811996908
            ====================================
        """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "فاتورة #${invoice.invoiceNumber}")
            putExtra(Intent.EXTRA_TEXT, printText)
        }
        context.startActivity(Intent.createChooser(shareIntent, "طباعة / مشاركة الفاتورة"))
    }

    private fun generateInvoiceHtml(
        invoice: Invoice,
        items: List<InvoiceItem>,
        currencySymbol: String
    ): String {
        val rows = items.joinToString("") { item ->
            """
            <tr>
                <td style="padding: 8px; border-bottom: 1px solid #ddd;">${item.productName}</td>
                <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: center;">${item.quantity}</td>
                <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">${item.unitPrice} $currencySymbol</td>
                <td style="padding: 8px; border-bottom: 1px solid #ddd; text-align: right;">${item.totalPrice} $currencySymbol</td>
            </tr>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: sans-serif; margin: 20px; color: #333; line-height: 1.5; }
                .header { text-align: center; border-bottom: 2px solid #0D1B2A; padding-bottom: 15px; margin-bottom: 20px; }
                .company-title { font-size: 24px; font-weight: bold; color: #0D1B2A; }
                .dev-subtitle { font-size: 13px; color: #555; }
                .meta-table { width: 100%; margin-bottom: 20px; }
                .meta-table td { padding: 4px; font-size: 14px; }
                .items-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }
                .items-table th { background: #0D1B2A; color: #fff; padding: 8px; text-align: right; }
                .totals { margin-top: 15px; width: 50%; margin-right: auto; }
                .totals td { padding: 6px; font-size: 14px; }
                .footer { margin-top: 40px; text-align: center; font-size: 12px; color: #777; border-top: 1px dashed #aaa; padding-top: 10px; }
            </style>
        </head>
        <body>
            <div class="header">
                <div class="company-title">فاتورة مبيعات معتمدة</div>
                <div class="dev-subtitle">نظام المحاسبة الشامل والفوترة السريعة</div>
            </div>

            <table class="meta-table">
                <tr>
                    <td><strong>رقم الفاتورة:</strong> #${invoice.invoiceNumber}</td>
                    <td><strong>التاريخ:</strong> ${LanguageManager.formatDate(invoice.createdAt, true)}</td>
                </tr>
                <tr>
                    <td><strong>اسم العميل:</strong> ${invoice.customerName.ifBlank { "عميل نقدي" }}</td>
                    <td><strong>رقم الهاتف:</strong> ${invoice.customerPhone.ifBlank { "-" }}</td>
                </tr>
                <tr>
                    <td><strong>المسؤول:</strong> ${invoice.createdBy}</td>
                    <td><strong>حالة الدفع:</strong> ${if (invoice.remainingDebt > 0) "آجل (دين)" else "مدفوع بالكامل"}</td>
                </tr>
            </table>

            <table class="items-table">
                <thead>
                    <tr>
                        <th>المادة / الصنف</th>
                        <th style="text-align: center;">الكمية</th>
                        <th style="text-align: right;">سعر الوحدة</th>
                        <th style="text-align: right;">المجموع</th>
                    </tr>
                </thead>
                <tbody>
                    $rows
                </tbody>
            </table>

            <table class="totals">
                <tr>
                    <td><strong>المجموع الإجمالي:</strong></td>
                    <td style="text-align: right; font-weight: bold;">${invoice.totalAmount} $currencySymbol</td>
                </tr>
                <tr>
                    <td><strong>الضريبة (${invoice.taxRate}%):</strong></td>
                    <td style="text-align: right;">${invoice.taxAmount} $currencySymbol</td>
                </tr>
                <tr>
                    <td><strong>المبلغ المدفوع:</strong></td>
                    <td style="text-align: right; color: green; font-weight: bold;">${invoice.paidAmount} $currencySymbol</td>
                </tr>
                <tr>
                    <td><strong>المتبقي بذمة العميل:</strong></td>
                    <td style="text-align: right; color: red; font-weight: bold;">${invoice.remainingDebt} $currencySymbol</td>
                </tr>
            </table>

            <div class="footer">
                شكراً لتعاملكم معنا ونسعد بخدمتكم دائماً.<br>
                برمجة وتطوير: <strong>أحمد فرحان السلماني</strong> | هاتف الدعم الفني: <strong>07811996908</strong>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
