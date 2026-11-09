package com.goktug.cuzdanim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Telefon yeniden başladığında veya uygulama güncellendiğinde hatırlatıcıları tekrar kurar. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" ->
                Reminders.reschedule(context, null)
        }
    }
}
