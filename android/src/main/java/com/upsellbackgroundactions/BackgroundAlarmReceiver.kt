package com.upsellbackgroundactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.upsellbackgroundactions.Helper.start


class BackgroundAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    println("This is intent action" + intent.action)
    if (ACTION_START_ALARM_MANAGER == intent.action) {
      try {
        start(context) // start the process again, this automatically stops other broadcast
        println("Start success in BackgroundAlarmReciever")
      } catch (e: Exception) {
        println("Exception from background Alarm Receiver$e")
      }
      //            currentServiceIntent.putExtras(bgOptions.getExtras());
//            context.startService(currentServiceIntent);
    }
  }

  companion object {
    private const val ACTION_START_ALARM_MANAGER =
      "com.asterinet.react.bgactions.ACTION_START_ALARM_MANAGER"
    private const val ACTION_STOP_SERVICE = "com.asterinet.react.bgactions.ACTION_STOP_SERVICE"
    private const val SHARED_PREFERENCES_KEY =
      "com.asterinet.react.bgactions.SHARED_PREFERENCES_KEY"
  }
}
