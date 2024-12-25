package com.upsellbackgroundactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Thread.State


class BackgroundAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    CoroutineScope(Dispatchers.Default).launch {
      println("This is intent action" + intent.action)
      if (StateSingleton.getInstance().ACTION_START_ALARM_MANAGER == intent.action) {
        try {
          StateSingleton.getInstance().setisItSafeToStopAlarm(false)
          println("This is isBackgroundServiceRunning" + StateSingleton.getInstance().isBackgroundServiceRunning)
          println("Value of StateSingleton listRunningServices"+{StateSingleton.getInstance().listRunningServices(context)})
          if(StateSingleton.getInstance().listRunningServices(context)=="[]"){
            StateSingleton.getInstance().sendStopBroadcast()
            // 3. Start the process again if it got turned off by the system
            try {
              val currentServiceIntent = Intent(context, RNBackgroundActionsTask::class.java)
              StateSingleton.getInstance().setIsBackgroundServiceRunning(false, null)
              Log.d("BackgroundAlarmReceiver", "Passed setIsBackgroundServiceRunning false")
              currentServiceIntent.putExtras(StateSingleton.getInstance().getBGOptions().extras!!)
              Thread.sleep(5000)
              StateSingleton.getInstance().setIsBackgroundServiceRunning(true, null)
              val timeValue= StateSingleton.getInstance().getAlarmTime()
              StateSingleton.getInstance().startAlarm(timeValue,context)
              context.startService(currentServiceIntent)
              StateSingleton.getInstance().setisItSafeToStopAlarm(true)
              // 1. Shut down the while loop
            } catch (e: java.lang.Exception) {
              Log.d("Error in BackgroundAlarmReceiver", e.toString())
            }
          }else{
            Thread.sleep(5000)
            println("In else State listRunningServices"+{StateSingleton.getInstance().listRunningServices(context)})
            val timeValue= StateSingleton.getInstance().getAlarmTime()
            StateSingleton.getInstance().startAlarm(timeValue,context)
            StateSingleton.getInstance().setisItSafeToStopAlarm(true)
          }



        } catch (e: Exception) {
          println("Exception from background Alarm Receiver$e")
        }

      }
    }
  }
}
