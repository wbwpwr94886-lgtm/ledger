package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.TimeUnit

class InventoryCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val database = AppDatabase.getDatabase(context, scope)
            val productDao = database.productDao()

            // Fetch current products from Room database synchronously
            val products: List<Product> = productDao.getAllProductsSync()

            // Check for items where stock quantity is at or below user's alert threshold
            val lowStockItems = products.filter { product ->
                product.stockQuantity <= product.minStockAlert
            }

            if (lowStockItems.isNotEmpty()) {
                sendLowStockNotification(lowStockItems.size, lowStockItems.map { it.name })
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun sendLowStockNotification(count: Int, itemNames: List<String>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "inventory_low_stock_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تنبيهات نواقص المخزون",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات دورية عند وصول المواد لحد إعادة الطلب"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val previewItems = itemNames.take(3).joinToString("، ")
        val contentText = if (count == 1) {
            "المنتج (${itemNames.first()}) وصل إلى الحد الأدنى للمخزون!"
        } else {
            "يوجد $count أصناف شارفت على النفاد: $previewItems"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.company_logo)
            .setContentTitle("⚠️ تنبيه نقص المخزون - ليدجر")
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$contentText.\nيرجى مراجعة إدارة المخزون وطلب كميات جديدة لتفادي نفاد البضائع.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "periodic_inventory_check_work"
        private const val NOTIFICATION_ID = 1001

        /**
         * Enqueues or replaces a periodic background check using WorkManager.
         * The default recurring interval is every 4 hours with battery constraints.
         */
        fun enqueuePeriodicInventoryCheck(
            context: Context,
            intervalHours: Long = 4,
            forceReplace: Boolean = false
        ) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<InventoryCheckWorker>(
                intervalHours, TimeUnit.HOURS,
                15, TimeUnit.MINUTES // Flex interval
            )
                .setConstraints(constraints)
                .addTag("inventory_check")
                .build()

            val policy = if (forceReplace) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                policy,
                workRequest
            )
        }

        /**
         * Enqueues a one-time immediate check for testing or on-demand execution.
         */
        fun runImmediateInventoryCheck(context: Context) {
            val oneTimeRequest = OneTimeWorkRequestBuilder<InventoryCheckWorker>()
                .addTag("inventory_check_instant")
                .build()

            WorkManager.getInstance(context).enqueue(oneTimeRequest)
        }

        /**
         * Cancels periodic background work.
         */
        fun cancelPeriodicInventoryCheck(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
