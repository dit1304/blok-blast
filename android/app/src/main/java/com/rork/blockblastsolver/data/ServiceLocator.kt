package com.rork.blockblastsolver.data

import android.content.Context

/** Minimal manual singleton wiring; the app only has one repository. */
object ServiceLocator {

    @Volatile
    private var historyRepository: HistoryRepository? = null

    fun history(context: Context): HistoryRepository =
        historyRepository ?: synchronized(this) {
            historyRepository ?: HistoryRepository(context.applicationContext).also {
                historyRepository = it
            }
        }
}
