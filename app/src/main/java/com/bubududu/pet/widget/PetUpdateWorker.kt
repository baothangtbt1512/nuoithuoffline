package com.bubududu.pet.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bubududu.pet.data.PetRepository

class PetUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val repo = PetRepository(applicationContext)
        val state = repo.loadState()
        PetWidgetProvider.updateAllWidgets(applicationContext, state)
        return Result.success()
    }
}
