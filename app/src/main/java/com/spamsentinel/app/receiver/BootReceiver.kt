package com.spamsentinel.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.spamsentinel.app.domain.ml.LinearSpamModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val model = LinearSpamModel.getInstance()
                    model.init(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
