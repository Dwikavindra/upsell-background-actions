package com.upsellbackgroundactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class BackgroundAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    CoroutineScope(Dispatchers.IO).launch {
      println("This is intent action" + intent.action)
      if (Names().ACTION_START_ALARM_MANAGER== intent.action) {
        if(StateSingleton.getInstance(context).getIsAlarmStoppedByUser()){
          StateSingleton.getInstance(context ).setIsBackgroundServiceRunning(false,null)//esnure while loop is stopped might as well be
          //ensure truly that alarm is stopped the method above safe to stop alarm is still needed if we need to cancel the alarm immediately,
          // however in the case that it fails this should stop it regardless
          // this will only be true if the user activates it from the ui not when restarting, when restarting via this receiver  the value is always false
          return@launch
        }
        try {
          StateSingleton.getInstance(context).setisItSafeToStopAlarm(false)
          println("This is isBackgroundServiceRunning" + StateSingleton.getInstance(context).isBackgroundServiceRunning())
          println("Value of StateSingleton listRunningServices"+{StateSingleton.getInstance(context).listRunningServices()})
            StateSingleton.getInstance(context).sendStopBroadcast()
            // 3. Start the process again if it got turned off by the system
            try {
              val currentServiceIntent = Intent(context, RNBackgroundActionsTaskTesting::class.java)
              StateSingleton.getInstance(context).setIsBackgroundServiceRunning(false, null)
              Log.d("BackgroundAlarmReceiver", "Passed setIsBackgroundServiceRunning false")
              currentServiceIntent.putExtras(StateSingleton.getInstance(context).getBGOptions().extras!!)
              Thread.sleep(20000)
              StateSingleton.getInstance(context).setIsBackgroundServiceRunning(true, null)
              context.startService(currentServiceIntent)
              StateSingleton.getInstance(context).setisItSafeToStopAlarm(true)
              // 1. Shut down the while loop
            } catch (e: java.lang.Exception) {
              Log.d("Error in BackgroundAlarmReceiver", e.toString())
            }



        } catch (e: Exception) {
          println("Exception from background Alarm Receiver$e")
        }

      }
    }
  }
}
