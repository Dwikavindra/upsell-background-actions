package com.upsellbackgroundactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking

class BackgroundAlarmReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
      val restartTaskIntent = Intent(context, RestartTask::class.java)
      if (Names().ACTION_START_ALARM_MANAGER == intent.action) {
          println("Received Action Start Alarm Manager")
          context.startForegroundService(restartTaskIntent)
        }
    }
}
