package com.spamsentinel.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spamsentinel.app.SpamSentinelApp
import com.spamsentinel.app.domain.agents.FeedbackAgent

class RetrainWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SpamSentinelApp ?: return Result.failure()
        val repository = app.container.repository

        return try {
            val promoted = FeedbackAgent.processFeedbackAndFineTune(applicationContext, repository)
            if (promoted) Result.success() else Result.failure()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
