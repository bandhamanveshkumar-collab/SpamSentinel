package com.spamsentinel.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.domain.model.VerdictAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.getLongExtra(NotificationHelper.EXTRA_MESSAGE_ID, -1L)
        if (messageId == -1L) return

        val app = context.applicationContext as? SpamSentinelApp ?: return
        val repository = app.container.repository

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = repository.messageDao.getMessageById(messageId)
                if (message != null) {
                    when (intent.action) {
                        NotificationHelper.ACTION_NOT_SPAM -> {
                            repository.updateMessageLabel(
                                id = messageId,
                                userLabel = VerdictAction.ALLOW,
                                newAction = VerdictAction.ALLOW
                            )
                            repository.recordFeedback(
                                messageId = messageId,
                                sender = message.sender,
                                body = message.body,
                                originalProbability = message.spamProbability,
                                correctedLabel = VerdictAction.ALLOW
                            )
                            repository.addSenderRule(message.sender, VerdictAction.ALLOW, "Marked safe by user")
                        }
                        NotificationHelper.ACTION_BLOCK_SENDER -> {
                            repository.updateMessageLabel(
                                id = messageId,
                                userLabel = VerdictAction.BLOCK,
                                newAction = VerdictAction.BLOCK
                            )
                            repository.recordFeedback(
                                messageId = messageId,
                                sender = message.sender,
                                body = message.body,
                                originalProbability = message.spamProbability,
                                correctedLabel = VerdictAction.BLOCK
                            )
                            repository.addSenderRule(message.sender, VerdictAction.BLOCK, "Blocked sender by user")
                        }
                    }
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(messageId.toInt())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
