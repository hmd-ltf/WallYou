package com.bnyro.wallpaper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bnyro.wallpaper.util.Preferences
import com.bnyro.wallpaper.util.WorkerHelper

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in BOOT_ACTIONS) return

        // WorkManager reschedules its periodic work after a reboot by itself, so only make sure
        // the work exists here and leave its existing schedule alone.
        WorkerHelper.enqueueOrCancelAll(context, Preferences.getWallpaperConfigs())
    }

    companion object {
        private val BOOT_ACTIONS = listOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON"
        )
    }
}
