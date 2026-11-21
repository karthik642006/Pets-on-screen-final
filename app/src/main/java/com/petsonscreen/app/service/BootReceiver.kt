package com.petsonscreen.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.petsonscreen.app.data.PetPreferencesRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = PetPreferencesRepository.getInstance(context).state.value
            if (prefs.enabled) {
                PetOverlayService.syncServiceState(context, prefs)
            }
        }
    }
}
