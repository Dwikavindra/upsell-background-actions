package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


object Helper {
  private const val ACTION_STOP_SERVICE = "com.asterinet.react.bgactions.ACTION_STOP_SERVICE"
  private const val ACTION_START_ALARM_MANAGER =
    "com.asterinet.react.bgactions.ACTION_START_ALARM_MANAGER"
  private const val SHARED_PREFERENCES_KEY = "com.asterinet.react.bgactions.SHARED_PREFERENCES_KEY"


  @SuppressLint("MissingPermission")
  @Throws(Exception::class)
  fun start(context: Context) {
    // Stop any other intent
    val thread = Thread {
      // Code to execute in the thread
      try {
        val stopIntent = Intent(ACTION_STOP_SERVICE)
        val optionSharedPreference = context.getSharedPreferences(
          SHARED_PREFERENCES_KEY,
          Context.MODE_PRIVATE
        )
        context.sendBroadcast(stopIntent) // stop the service regardless
        val unconvertedHashMap = optionSharedPreference.getString("optionHashMap", null)
        val triggerTime =
          optionSharedPreference.getLong("triggerTime", 300000) // defaults to 5 minutes
        val type = object :
          TypeToken<HashMap<String?, Any?>?>() {
        }.type
        val optionsHashMap = Gson()
          .fromJson<HashMap<String, Any>>(unconvertedHashMap, type)
        val options = Arguments.makeNativeMap(optionsHashMap)
        val bgOptions = BackgroundTaskOptions(context as ReactContext, options)
        val currentServiceIntent = Intent(
          context,
          RNBackgroundActionsTaskTesting::class.java
        )
        currentServiceIntent.putExtras(bgOptions.extras!!)
        // Start the task
        // put before start service
        // to esnure when run commands run it works
        println("Sleeping for 5 seconds")
        Thread.sleep(5000) // put sleep for 5 seconds
        println("Passed 5 seconds")
        context.startService(currentServiceIntent)
        optionSharedPreference.edit().putBoolean("isBackgroundServiceRunning", true).apply()
        val alarmReciever: BroadcastReceiver = BackgroundAlarmReceiver()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          println("inside start function helper Build SDK is over Lollipop")
          var alarmManager: AlarmManager? = null
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager = context.getSystemService(AlarmManager::class.java)
          }
          if (alarmManager != null) {
            println("inside start function helper alarm manager is not null")
            val alarmClockInfo =
              AlarmClockInfo(System.currentTimeMillis() + triggerTime, null)
            val startAlarmIntent = Intent(
              context,
              BackgroundAlarmReceiver::class.java
            )
            startAlarmIntent.setAction(ACTION_START_ALARM_MANAGER)
            val pendingIntent = PendingIntent.getBroadcast(
              context,
              0,
              startAlarmIntent,
              PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            println("inside start function helper Passed set Alarm Clock")
          } else {
            throw Exception("Alarm manager is null")
          }
        } else {
          throw Exception("OS version needs to be larger than android lollipop or android 21")
        }
      } catch (e: Exception) {
        println("Exception at helper start$e")
      }
    }
    thread.start()
  }
}
