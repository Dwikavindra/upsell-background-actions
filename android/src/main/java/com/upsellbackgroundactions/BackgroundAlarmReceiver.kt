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
          //restart the printing process // new alarm would be set in the module no need to stop the alarm cause thats one shot so if by the time you get here it shouldn'
          // fireoff again
          // 1. Shut down the while loop
          StateSingleton.getInstance().setIsBackgroundServiceRunning(false, null)
          Log.d("BackgroundAlarmReceiver", "Passed setIsBackgroundServiceRunning false")
          // 2. Send stopBroadcast

          println("This is isBackgroundServiceRunning" + StateSingleton.getInstance().isBackgroundServiceRunning)
          StateSingleton.getInstance().sendStopBroadcast()
          // 3. Start the process again
          try {
            // Stop any other inten
            val reactContext = StateSingleton.getInstance().getReactContext()
            val optionSharedPreference =
              reactContext.getSharedPreferences(
                StateSingleton.getInstance().SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
              )
            val currentServiceIntent = Intent(reactContext, RNBackgroundActionsTask::class.java)
            currentServiceIntent.putExtras(StateSingleton.getInstance().getBGOptions().extras!!)
            Thread.sleep(5000)
            StateSingleton.getInstance().setIsBackgroundServiceRunning(true, null)
            StateSingleton.getInstance().setisItSafeToStopAlarm(true)
            reactContext.startService(currentServiceIntent)
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
