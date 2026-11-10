package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.AmpSubscriber
import com.example.data.model.Customer
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import java.net.URLEncoder

object WhatsAppHelper {

    fun sendInvoiceNotification(
        context: Context,
        customerPhone: String,
        customerName: String,
        invoice: Invoice,
        items: List<InvoiceItem>,
        currencySymbol: String
    ) {
        val itemsSummary = if (items.isNotEmpty()) {
            items.joinToString("\n") {
                "  • ${it.productName} × ${it.quantity} = ${it.totalPrice} $currencySymbol"
            }
        } else {
            "  • خدمات ومشتريات متنوعة"
        }

        val message = """
            🧾 *فاتورة مبيعات إلكترونية*
            *نظام ليدجر للمحاسبة | Ledger System*
            ━━━━━━━━━━━━━━━━━━━━━
            👤 *العميل:* $customerName
            🔢 *رقم الفاتورة:* #${invoice.invoiceNumber}
            📅 *التاريخ:* ${LanguageManager.formatDate(invoice.createdAt, true)}
            
            📦 *تفاصيل المواد والبنود:*
            $itemsSummary
            
            💰 *المجموع الإجمالي:* ${invoice.totalAmount} $currencySymbol
            💵 *المدفوع نقداً:* ${invoice.paidAmount} $currencySymbol
            ⚠️ *المتبقي (آجل/دين):* ${invoice.remainingDebt} $currencySymbol
            ━━━━━━━━━━━━━━━━━━━━━
            🙏 شكراً لحسن تعاملكم وثقتكم بنا.
            💼 ليدجر - النظام المحاسبي الذكي
            📞 هاتف الدعم: 07811996908
        """.trimIndent()

        openWhatsApp(context, customerPhone, message)
    }

    /**
     * Send payment receipt directly to customer via WhatsApp intent
     */
    fun sendPaymentReceipt(
        context: Context,
        customerPhone: String,
        customerName: String,
        receiptNumber: String,
        amount: Double,
        currencySymbol: String,
        remainingDebt: Double,
        paymentMethod: String = "نقدي",
        notes: String = ""
    ) {
        val notesLine = if (notes.isNotBlank()) "\n📝 *ملاحظات:* $notes" else ""
        val debtStatusLine = if (remainingDebt > 0) {
            "⚠️ *الرصيد المتبقي بذمتكم:* $remainingDebt $currencySymbol"
        } else {
            "✅ *حالة الحساب:* تم تسديد كامل المستحقات براءة ذمة"
        }

        val message = """
            🧾 *سند قبض إلكتروني (إيصال تسديد)*
            *نظام ليدجر للمحاسبة | Ledger System*
            ━━━━━━━━━━━━━━━━━━━━━
            👤 *استلمنا من السيد/ة:* $customerName
            🔢 *رقم السند:* #$receiptNumber
            📅 *التاريخ والوقت:* ${LanguageManager.formatDate(System.currentTimeMillis(), true)}
            
            💵 *المبلغ المقبوض:* $amount $currencySymbol
            💳 *طريقة الدفع:* $paymentMethod$notesLine
            
            ━━━━━━━━━━━━━━━━━━━━━
            $debtStatusLine
            ━━━━━━━━━━━━━━━━━━━━━
            🙏 شكراً لوفائكم والتزامكم بالتسديد.
            💼 ليدجر - الإدارة المالية الذكية
        """.trimIndent()

        openWhatsApp(context, customerPhone, message)
    }

    /**
     * Send debt reminder with payment details via WhatsApp
     */
    fun sendDebtReminder(
        context: Context,
        customer: Customer,
        currencySymbol: String
    ) {
        val dueDateText = customer.dueDateMillis?.let {
            "📅 *تاريخ الاستحقاق:* ${LanguageManager.formatDateShort(it, true)}"
        } ?: "يرجى التكرم بتسديد المبلغ عند توفره."

        val message = """
            🔔 *إشعار تذكير بمستحقات مالية*
            *نظام ليدجر للمحاسبة | Ledger System*
            ━━━━━━━━━━━━━━━━━━━━━
            تحية طيبة، عزيزي الزبون: *${customer.name}*
            
            نود تذكيركم بلطف بالرصيد القائم بذمتكم:
            💰 *المبلغ المستحق:* ${customer.currentDebt} $currencySymbol
            $dueDateText
            
            ━━━━━━━━━━━━━━━━━━━━━
            شاكرين حسن تعاونكم الدائم وتواصلكم معنا.
            للاستفسار أو التسديد يرجى الرد على هذه الرسالة.
            💼 ليدجر - المطور: أحمد فرحان السلماني
        """.trimIndent()

        openWhatsApp(context, customer.phone, message)
    }

    /**
     * Send customer complete account ledger statement via WhatsApp
     */
    fun sendAccountStatement(
        context: Context,
        customer: Customer,
        invoicesCount: Int,
        receiptsCount: Int,
        currencySymbol: String
    ) {
        val message = """
            📋 *كشف حساب مالي دوري*
            *نظام ليدجر للمحاسبة | Ledger System*
            ━━━━━━━━━━━━━━━━━━━━━
            👤 *العميل:* ${customer.name}
            📞 *الهاتف:* ${customer.phone.ifBlank { "-" }}
            📅 *تاريخ الكشف:* ${LanguageManager.formatDateShort(System.currentTimeMillis(), true)}
            
            📊 *ملخص العمليات:*
            • عدد الفواتير السابقة: $invoicesCount فاتورة
            • عدد سندات القبض: $receiptsCount سند
            
            💰 *إجمالي الرصيد المستحق (الدين):* ${customer.currentDebt} $currencySymbol
            ━━━━━━━━━━━━━━━━━━━━━
            💼 صادر من نظام ليدجر للمحاسبة
        """.trimIndent()

        openWhatsApp(context, customer.phone, message)
    }

    fun sendAmpereBill(
        context: Context,
        subscriber: AmpSubscriber,
        currencySymbol: String
    ) {
        val statusText = if (subscriber.isBillDelivered) "تم توصيل الفاتورة 🟢" else "قيد التوصيل 🔴"
        val message = """
            *فاتورة اشتراك المولد الكهربائي* ⚡
            -------------------------------
            المشترك: *${subscriber.name}*
            عدد الأمبيرات: *${subscriber.amperes} أمبير*
            سعر الأمبير: ${subscriber.pricePerAmpere} $currencySymbol
            الموقع / الجوزة: ${subscriber.meterNumberOrLocation.ifBlank { "الرئيسية" }}
            
            *المبلغ الشهري:* ${subscriber.monthlyTotal} $currencySymbol
            *الديون السابقة:* ${subscriber.debtAmount} $currencySymbol
            *إجمالي المطلوب:* ${subscriber.monthlyTotal + subscriber.debtAmount} $currencySymbol
            حالة الوصل: $statusText
            -------------------------------
            للاستفسار أو التسديد يرجى الاتصال بنا.
            نظام إدارة الأمبيرات - المطور: أحمد فرحان السلماني
        """.trimIndent()

        openWhatsApp(context, subscriber.phone, message)
    }

    fun contactDeveloperSupport(context: Context) {
        val message = "السلام عليكم ورحمة الله، أحتاج مساعدة أو استفسار بخصوص تطبيق المحاسبة الشامل للمطور أحمد فرحان السلماني."
        openWhatsApp(context, "07811996908", message)
    }

    fun sendDirectMessage(context: Context, phone: String, message: String) {
        openWhatsApp(context, phone, message)
    }

    fun openWhatsApp(context: Context, phone: String, message: String) {
        try {
            var formattedPhone = phone.replace("+", "").replace(" ", "").replace("-", "")
            if (formattedPhone.startsWith("07")) {
                // Iraq international prefix
                formattedPhone = "964" + formattedPhone.substring(1)
            }
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            // If WhatsApp is not installed or network fails, try generic share
            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                }
                context.startActivity(Intent.createChooser(shareIntent, "مشاركة الفاتورة"))
            } catch (ex: Exception) {
                Toast.makeText(context, "تعذر فتح تطبيق المراسلة", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
