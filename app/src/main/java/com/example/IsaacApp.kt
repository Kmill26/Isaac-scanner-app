package com.example

import android.app.Application
import com.example.data.model.IsaacItemDatabase
import kotlin.concurrent.thread

/**
 * Custom [Application] that wires the app context into [IsaacItemDatabase] and warms the
 * bundled 721-item catalog on a background thread so the first screen access is instant.
 *
 * The parse (~50-80 ms for the 436 KB asset) never runs on the main thread here: `onCreate`
 * only kicks off the warm-up thread. Should the UI reach `IsaacItemDatabase.items` before the
 * warm-up finishes, the `by lazy` guard makes it block on that same parse — correct, just not
 * yet cached.
 */
class IsaacApp : Application() {
    override fun onCreate() {
        super.onCreate()
        IsaacItemDatabase.install(this)
        thread(name = "isaac-catalog-warmup", isDaemon = true) {
            runCatching { IsaacItemDatabase.warmUp() }
        }
    }
}
