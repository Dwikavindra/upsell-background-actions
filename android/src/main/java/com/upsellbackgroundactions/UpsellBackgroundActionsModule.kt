package com.upsellbackgroundactions
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import java.lang.Thread.State
import java.util.concurrent.locks.ReentrantLock


class UpsellBackgroundActionsModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  private  val SCHEDULE_EXACT_ALARM_REQUEST: Int = 1
  private var currentServiceIntent: Intent? = null
  private var alarmManager: AlarmManager? = null
  private var alarmPendingIntent: PendingIntent? = null
  private var mExactAlarmPromise: Promise? = null
  private val semaphore=Semaphore(1)
  private val addPrinterSemaphore=Semaphore(1)
  init {
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
    StateSingleton.getInstance().setReactContext(reactApplicationContext)
    reactApplicationContext.addActivityEventListener(mActivityEventListener);
      this.alarmManager = reactApplicationContext.getSystemService(
        AlarmManager::class.java
      )

  }
  companion object {
    const val NAME = "UpsellBackgroundActions"
  }

  @ReactMethod
  fun setCallBack(callback: Callback){
    StateSingleton.getInstance().setCallBack(callback)
  }
  @SuppressLint("ServiceCast")
  @ReactMethod
  fun listRunningServices(promise: Promise) {
    try {
      val activityManager = reactApplicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

      val services = runningServices
        .filter { it.service.packageName == reactApplicationContext.packageName }
        .map { it.service.className }

      promise.resolve(services.toString())
    } catch (e: Exception) {
      promise.reject("ERROR", e)
    }
  }

  @Suppress("unused")
  @ReactMethod
  fun start(options: ReadableMap, triggerTime: Double, promise: Promise) {

    CoroutineScope(Dispatchers.Main).launch{
      try {
        // Stop any other inten

        val bgOptions = BackgroundTaskOptions(reactApplicationContext, options)
        currentServiceIntent = Intent(reactApplicationContext, RNBackgroundActionsTask::class.java)
        StateSingleton.getInstance().setCurrentServiceIntent(currentServiceIntent!!)
        StateSingleton.getInstance().setBGOptions(bgOptions)
        currentServiceIntent!!.putExtras(bgOptions.extras!!)
        StateSingleton.getInstance().startAlarm(triggerTime,this@UpsellBackgroundActionsModule.reactApplicationContext)
        StateSingleton.getInstance().setIsBackgroundServiceRunning(true,null)
        reactApplicationContext.startService(currentServiceIntent)
      } catch (e: java.lang.Exception) {
        promise.reject(e)
      }
    }
    }



  @Suppress("unused")
  @ReactMethod
  fun checkScheduleExactAlarmPermission(promise: Promise) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      promise.resolve(this.alarmManager?.canScheduleExactAlarms())
    }
    promise.resolve(false)
  }
  @Suppress("unused")
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
        this.alarmManager!!.cancel(this.alarmPendingIntent!!)
        promise.resolve(null)
      } else {
        throw java.lang.Exception(
          ("""Alarm Manager not canceled Status of AlarmManager:${this.alarmManager}""").toString() + "Status of Pending Intent" + this.alarmPendingIntent
        )
      }
    } catch (e: java.lang.Exception) {
      promise.reject(e)
    }
  }
  @Suppress("unused")
  @ReactMethod
  fun getIsItSafeToStopAlarm(promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val value= async { StateSingleton.getInstance().getisItSafeToStopAlarm() }
        promise.resolve(value.await())
      } catch (e: java.lang.Exception) {
        promise.reject(e)
      }
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
      notificationManager.notify(StateSingleton.getInstance().SERVICE_NOTIFICATION_ID, notification)
    } catch (e: java.lang.Exception) {
      promise.reject(e)
      return
    }
    promise.resolve(null)
  }
  @Suppress("unused")
  @ReactMethod
  fun isBackgroundServiceRunning(promise: Promise) {
    StateSingleton.getInstance().isBackgroundServiceRunning(promise)
  }
  @Suppress("unused")
  @ReactMethod
  fun setIsBackgroundServiceRunning(value:Boolean,promise: Promise) {
    StateSingleton.getInstance().setIsBackgroundServiceRunning(value,promise)
  }
  @Suppress("unused")
  @ReactMethod
  fun sendStopBroadcast(promise: Promise) {
    try {
      val stopIntent= Intent(StateSingleton.getInstance().ACTION_STOP_SERVICE)
      reactApplicationContext.sendBroadcast(stopIntent)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject(e)

    }
  }

  @SuppressLint("MissingPermission")
  @Suppress("unused")
  @ReactMethod
  fun setAlarm(triggerTime: Double,promise: Promise) {
    try {
      val convertedTriggerTime = triggerTime.toLong()
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        println("inside start function Build SDK is over Lollipop")
        if (alarmManager != null) {
          println("inside start function alarm manager is not null")
          val alarmClockInfo =
            AlarmClockInfo(System.currentTimeMillis() + convertedTriggerTime, null)
          val startAlarmIntent = Intent(
            reactApplicationContext,
            BackgroundAlarmReceiver::class.java
          )
          startAlarmIntent.setAction(StateSingleton.getInstance().ACTION_START_ALARM_MANAGER)
          val pendingIntent = PendingIntent.getBroadcast(
            reactApplicationContext,
            0,
            startAlarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )
          StateSingleton.getInstance().setPendingIntent(pendingIntent)
          this@UpsellBackgroundActionsModule.alarmPendingIntent = pendingIntent
          this@UpsellBackgroundActionsModule.alarmManager!!.setAlarmClock(alarmClockInfo, pendingIntent)
          println("inside start function Passed set Alarm Clock")
        } else {
          throw java.lang.Exception("Alarm manager is null")
        }
        promise.resolve(null)
      } else {
        throw java.lang.Exception("OS version needs to be larger than android lollipop or android 21")
      }
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject(e)

    }
  }



  @Suppress("unused")
  @ReactMethod
  fun lock(promise: Promise) {
    CoroutineScope(Dispatchers.Main).launch {
      try {
        this@UpsellBackgroundActionsModule.semaphore.acquire()
        promise.resolve(null)
      } catch (e: Exception) {
        promise.reject(e)

      }
    }

  }

  @Suppress("unused")
  @ReactMethod
  fun unlock(promise: Promise) {
    CoroutineScope(Dispatchers.Main).launch {
      try {
        this@UpsellBackgroundActionsModule.semaphore.release()
        promise.resolve(null)
      } catch (e: Exception) {
        promise.reject(e)

      }
    }
  }
  @Suppress("unused")
  @ReactMethod
  fun lockAddPrinterSemaphore(promise: Promise) {
    CoroutineScope(Dispatchers.Main).launch {
      try {
        this@UpsellBackgroundActionsModule.addPrinterSemaphore.acquire()
        promise.resolve(null)
      } catch (e: Exception) {
        promise.reject(e)

      }
    }

  }

  @Suppress("unused")
  @ReactMethod
  fun unlockAddPrinterSemaphore(promise: Promise) {
    CoroutineScope(Dispatchers.Main).launch {
      try {
        this@UpsellBackgroundActionsModule.addPrinterSemaphore.release()
        promise.resolve(null)
      } catch (e: Exception) {
        promise.reject(e)

      }
    }
  }
  @Suppress("unused")
  @ReactMethod
  fun stopAlarm(promise:Promise){
      StateSingleton.getInstance().stopAlarm(promise)
  }
  @ReactMethod
  fun getAlarmPermissionStatus(promise: Promise) {
    val optionSharedPreference =
      reactApplicationContext.getSharedPreferences(StateSingleton.getInstance().SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
    promise.resolve(optionSharedPreference.getBoolean("ALARM_PERMISSION_GRANTED", false))
  }
}
