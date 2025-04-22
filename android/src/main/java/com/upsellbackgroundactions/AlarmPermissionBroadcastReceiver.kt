package com.upsellbackgroundactions

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.facebook.react.bridge.Arguments


class AlarmPermissionBroadcastReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    println("IN_STATE_SCHEDULE_EXACT_ALARM_PERMISSION_CHANGED")
    println("This is inten action " + intent.action)
    if (AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED == intent.action) {
      println("IN_STATE_SCHEDULE_EXACT_ALARM_PERMISSION_CHANGED")

      val result = Arguments.createMap()
      result.putBoolean("status", true)
      val optionSharedPreference =
        context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
      optionSharedPreference.edit().putBoolean("ALARM_PERMISSION_GRANTED", true).apply()
    }
  }

  companion object {
    private const val SHARED_PREFERENCES_KEY =
      "com.asterinet.react.bgactions.SHARED_PREFERENCES_KEY"
  }
}
