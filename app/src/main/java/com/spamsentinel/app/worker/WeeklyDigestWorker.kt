package com.spamsentinel.app.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.receiver.NotificationHelper
import kotlinx.coroutines.flow.first

class WeeklyDigestWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SpamSentinelApp ?: return Result.failure()
        val repository = app.container.repository

        val weeklyDigestEnabled = repository.preferences.weeklyDigestEnabled.first()
        if (!weeklyDigestEnabled) return Result.success()

        val totalScanned = repository.totalCount.first()
        val totalBlocked = repository.blockedCount.first()
        val totalReview = repository.reviewCount.first()

        NotificationHelper.createChannels(applicationContext)

        val title = "🛡️ SpamSentinel Weekly Digest"
        val content = "This week: $totalBlocked spam blocked, $totalReview items reviewed out of $totalScanned total SMS."

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_WEEKLY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)

        manager.notify(9999, builder.build())
        return Result.success()
    }
}
