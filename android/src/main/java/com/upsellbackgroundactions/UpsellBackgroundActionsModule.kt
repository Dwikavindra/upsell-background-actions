package com.upsellbackgroundactions

import android.app.Activity
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.google.gson.Gson


class UpsellBackgroundActionsModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android

  private val ACTION_STOP_SERVICE: String = "com.upsellbackgroundactions.ACTION_STOP_SERVICE"
  private val ACTION_START_ALARM_MANAGER: String = "com.upsellbackgroundactions.ACTION_START_ALARM_MANAGER"
  private val SHARED_PREFERENCES_KEY: String = "com.upsellbackgroundactions.SHARED_PREFERENCES_KEY"
  private  val SCHEDULE_EXACT_ALARM_REQUEST: Int = 1
  private var currentServiceIntent: Intent? = null
  private val alarmManager: AlarmManager? = null
  private var alarmPendingIntent: PendingIntent? = null
  private var mExactAlarmPromise: Promise? = null
  val mActivityEventListener = object : BaseActivityEventListener() {
    override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, intent: Intent?) {
      if (requestCode == SCHEDULE_EXACT_ALARM_REQUEST) {
        mExactAlarmPromise?.let {
          it.resolve("ACTIVITY_OPENED")
          mExactAlarmPromise = null
        }
      }
    }
  }
  companion object {
    const val NAME = "UpsellBackgroundActions"
  }

  @Suppress("unused")
  @ReactMethod
  fun start(options: ReadableMap, triggerTime: Double, promise: Promise) {
    try {
      // Stop any other intent
      val convertedTriggerTime = triggerTime.toLong()
      val stopIntent: Intent = Intent(ACTION_STOP_SERVICE)
      reactApplicationContext.sendBroadcast(stopIntent) // stop the service regardless
      if (currentServiceIntent != null) reactApplicationContext.stopService(currentServiceIntent)
      // Create the service
      currentServiceIntent = Intent(reactContext, RNBackgroundActionsTask::class.java)
      // Get the task info from the options
      val taskOptions = options.toHashMap() // convert options to HashMap
      val hashMapString = Gson().toJson(taskOptions)
      val optionSharedPreference =
        reactApplicationContext.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
      optionSharedPreference.edit().putString("optionHashMap", hashMapString)
        .apply() // save to hashmap
      optionSharedPreference.edit().putLong("triggerTime", convertedTriggerTime)
        .apply() // save to hashmap
      val bgOptions = BackgroundTaskOptions(reactApplicationContext, options)
      currentServiceIntent.putExtras(bgOptions.extras!!)
      // Start the task
      reactApplicationContext.startService(currentServiceIntent)
      optionSharedPreference.edit().putBoolean("isBackgroundServiceRunning", true).apply()
      // start an alarm manager for n minutes later
      val alarmReciever: BroadcastReceiver = BackgroundAlarmReceiver()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        println("inside start function Build SDK is over Lollipop")
        if (alarmManager != null) {
          println("inside start function alarm manager is not null")
          val alarmClockInfo =
            AlarmClockInfo(System.currentTimeMillis() + convertedTriggerTime, null)
          val startAlarmIntent = Intent(
            reactContext,
            BackgroundAlarmReceiver::class.java
          )
          startAlarmIntent.setAction(ACTION_START_ALARM_MANAGER)
          val pendingIntent = PendingIntent.getBroadcast(
            this.reactContext,
            0,
            startAlarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )
          this.alarmPendingIntent = pendingIntent
          this.alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
          println("inside start function Passed set Alarm Clock")
        } else {
          throw java.lang.Exception("Alarm manager is null")
        }
        promise.resolve(null)
      } else {
        throw java.lang.Exception("OS version needs to be larger than android lollipop or android 21")
      }
    } catch (e: java.lang.Exception) {
      promise.reject(e)
    }
  }
  @ReactMethod
  fun checkScheduleExactAlarmPermission(promise: Promise) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      promise.resolve(this.alarmManager.canScheduleExactAlarms())
    }
    promise.resolve(false)
  }
  @RequiresApi(api = Build.VERSION_CODES.S)
  @ReactMethod
  fun requestExactAlarmPermission(promise: Promise) {
    val currentActivity = currentActivity

    if (currentActivity == null) {
      promise.reject("ACTIVITY_DOESNT_EXIST", "Activity doesn't exist")
      return
    }

    // Store the promise to resolve/reject when picker returns data
    mExactAlarmPromise = promise

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        reactApplicationContext.registerReceiver(
          AlarmPermissionBroadcastReceiver(),
          IntentFilter(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED),
          Context.RECEIVER_EXPORTED
        )
        println("RECEIVER_EXPORTED")
      } else {
        reactApplicationContext.registerReceiver(
          AlarmPermissionBroadcastReceiver(),
          IntentFilter(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        )
        println("RECEIVER_ON_LOWER_SDK")
      }

      val scheduleExactAlarm = Intent("android.settings.REQUEST_SCHEDULE_EXACT_ALARM")
      scheduleExactAlarm.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      currentActivity.startActivityForResult(scheduleExactAlarm, SCHEDULE_EXACT_ALARM_REQUEST)
    } catch (e: java.lang.Exception) {
      mExactAlarmPromise!!.reject("FAILED_TO_SHOW_ALARM_SETTINGS", e)
      mExactAlarmPromise = null
    }
  }
  @Suppress("unused")
  @ReactMethod
  fun stop(promise: Promise) {
    try {
      if (currentServiceIntent != null) reactApplicationContext.stopService(currentServiceIntent)
      if (this.alarmPendingIntent != null && this.alarmManager != null) {
        this.alarmManager.cancel(this.alarmPendingIntent)
        promise.resolve(null)
      } else {
        throw java.lang.Exception(
          ("""Alarm Manager not canceled
 Status of AlarmManager:${this.alarmManager}""").toString() + "Status of Pending Intent" + this.alarmPendingIntent
        )
      }
    } catch (e: java.lang.Exception) {
      promise.reject(e)
    }
  }
  @Suppress("unused")
  @ReactMethod
  fun updateNotification(options: ReadableMap, promise: Promise) {
    // Get the task info from the options
    try {
      val bgOptions = BackgroundTaskOptions(reactApplicationContext, options)
      val notification = RNBackgroundActionsTask.buildNotification(
        reactApplicationContext, bgOptions
      )
      val notificationManager =
        reactApplicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      notificationManager.notify(RNBackgroundActionsTask.SERVICE_NOTIFICATION_ID, notification)
    } catch (e: java.lang.Exception) {
      promise.reject(e)
      return
    }
    promise.resolve(null)
  }
  @Suppress("unused")
  @ReactMethod
  fun isBackgroundServiceRunning(promise: Promise) {
    val optionSharedPreference =
      reactApplicationContext.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
    val result = optionSharedPreference.getBoolean("isBackgroundServiceRunning", false)
    promise.resolve(result)
  }
  @Suppress("unused")
  @ReactMethod
  fun sendStopBroadcast(promise: Promise) {
    try {
      val stopIntent: Intent = Intent(ACTION_STOP_SERVICE)
      val optionSharedPreference =
        reactApplicationContext.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
      optionSharedPreference.edit().putBoolean("isBackgroundServiceRunning", false).apply()
      reactApplicationContext.sendBroadcast(stopIntent)
      if (this.alarmPendingIntent != null && this.alarmManager != null) {
        this.alarmManager.cancel(this.alarmPendingIntent)
        promise.resolve(null)
      } else {
        throw Exception(
          ("""Alarm Manager not canceled
 Status of AlarmManager:${this.alarmManager}""").toString() + "Status of Pending Intent" + this.alarmPendingIntent
        )
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject(e)
    }
  }

  @ReactMethod
  fun getAlarmPermissionStatus(promise: Promise) {
    val optionSharedPreference =
      reactApplicationContext.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
    promise.resolve(optionSharedPreference.getBoolean("ALARM_PERMISSION_GRANTED", false))
  }


}
