package com.spamsentinel.app.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.spamsentinel.app.MainActivity
import com.spamsentinel.app.domain.model.SpamVerdict
import com.spamsentinel.app.domain.model.VerdictAction

object NotificationHelper {

    const val CHANNEL_SPAM = "spam_channel"
    const val CHANNEL_REVIEW = "review_channel"
    const val CHANNEL_SAFE = "safe_channel"
    const val CHANNEL_WEEKLY = "weekly_channel"

    const val ACTION_NOT_SPAM = "com.spamsentinel.app.ACTION_NOT_SPAM"
    const val ACTION_BLOCK_SENDER = "com.spamsentinel.app.ACTION_BLOCK_SENDER"
    const val EXTRA_MESSAGE_ID = "extra_message_id"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val spamChannel = NotificationChannel(
                CHANNEL_SPAM,
                "Spam Alert Detections",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications with vibration and sound for detected SMS spam/scams"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }

            val reviewChannel = NotificationChannel(
                CHANNEL_REVIEW,
                "Needs Review Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority notifications for suspicious messages needing review"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 100, 300)
                setSound(soundUri, audioAttributes)
                setShowBadge(true)
            }

            val safeChannel = NotificationChannel(
                CHANNEL_SAFE,
                "Safe Messages",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Low priority quiet notifications for safe messages"
                setShowBadge(false)
            }

            val weeklyChannel = NotificationChannel(
                CHANNEL_WEEKLY,
                "Weekly Reports",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Weekly spam protection summary digests"
                setShowBadge(false)
            }

            manager.createNotificationChannels(
                listOf(spamChannel, reviewChannel, safeChannel, weeklyChannel)
            )
        }
    }

    fun showVerdictNotification(
        context: Context,
        messageId: Long,
        sender: String,
        body: String,
        verdict: SpamVerdict
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannels(context)

        val channelId = when (verdict.action) {
            VerdictAction.BLOCK -> CHANNEL_SPAM
            VerdictAction.REVIEW -> CHANNEL_REVIEW
            VerdictAction.ALLOW -> CHANNEL_SAFE
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("messageId", messageId)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            messageId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notSpamIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_NOT_SPAM
            putExtra(EXTRA_MESSAGE_ID, messageId)
        }
        val notSpamPendingIntent = PendingIntent.getBroadcast(
            context,
            (messageId * 10 + 1).toInt(),
            notSpamIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val blockSenderIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_BLOCK_SENDER
            putExtra(EXTRA_MESSAGE_ID, messageId)
        }
        val blockSenderPendingIntent = PendingIntent.getBroadcast(
            context,
            (messageId * 10 + 2).toInt(),
            blockSenderIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val probPct = (verdict.spamProbability * 100).toInt()
        val topTermsText = verdict.topSpamTerms.joinToString(", ") { it.term }

        val title = when (verdict.action) {
            VerdictAction.BLOCK -> "🚨 SPAM ALERT: ${verdict.scamType.displayName} Blocked"
            VerdictAction.REVIEW -> "⚠️ REVIEW NEEDED: ${verdict.scamType.displayName}"
            VerdictAction.ALLOW -> "🛡️ Safe Message from $sender"
        }

        val contentText = when (verdict.action) {
            VerdictAction.BLOCK -> "From $sender ($probPct% risk). Terms: $topTermsText"
            VerdictAction.REVIEW -> "From $sender ($probPct% risk). $body"
            VerdictAction.ALLOW -> body
        }

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\n\nFull SMS: $body"))
            .setPriority(
                if (verdict.action == VerdictAction.ALLOW) NotificationCompat.PRIORITY_LOW
                else NotificationCompat.PRIORITY_MAX
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)

        if (verdict.action == VerdictAction.BLOCK || verdict.action == VerdictAction.REVIEW) {
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Not Spam", notSpamPendingIntent)
            builder.addAction(android.R.drawable.ic_delete, "Block Sender", blockSenderPendingIntent)
        }

        manager.notify(messageId.toInt(), builder.build())
    }
}
