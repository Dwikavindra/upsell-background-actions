package com.upsellbackgroundactions
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class UpsellBackgroundActionsModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return NAME
  }

  private val SCHEDULE_EXACT_ALARM_REQUEST: Int = 1
  private var currentServiceIntent: Intent? = null
  private var alarmManager: AlarmManager? = null
  private var alarmPendingIntent: PendingIntent? = null
  private var mExactAlarmPromise: Promise? = null

  init {
    val mActivityEventListener = object : BaseActivityEventListener() {
      override fun onActivityResult(
        activity: Activity,
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
      ) {
        if (requestCode == SCHEDULE_EXACT_ALARM_REQUEST) {
          mExactAlarmPromise?.let {
            it.resolve("ACTIVITY_OPENED")
            mExactAlarmPromise = null
          }
        }
      }
    }

    StateSingleton.getInstance(this.reactApplicationContext.applicationContext)
    reactApplicationContext.addActivityEventListener(mActivityEventListener);
    this.alarmManager = reactApplicationContext.getSystemService(
      AlarmManager::class.java
    )

  }

  companion object {
    const val NAME = "UpsellBackgroundActions"
  }

  @ReactMethod
  fun setCallBack(callback: Callback) {
    StateSingleton.getInstance(this.reactApplicationContext.applicationContext)
      .setCallBack(callback)
  }

  @ReactMethod
  fun isIgnoreBatteryOptimization(promise: Promise) {
    val packageName: String = reactApplicationContext.packageName
    val pm = reactApplicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
    promise.resolve(pm.isIgnoringBatteryOptimizations(packageName))

  }

  @SuppressLint("BatteryLife")
  @ReactMethod
  fun requestIgnoreBatteryOptmization(promise: Promise) {
    val intent = Intent()
    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
    intent.setData(Uri.parse("package:" + reactApplicationContext.packageName));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    startActivity(reactApplicationContext, intent, null)
    promise.resolve(null)
  }

  @ReactMethod
  fun requestActionIgnoreBatteryOptimizationSettings(promise: Promise) {
    val intent = Intent()
    intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    startActivity(reactApplicationContext, intent, null)
    promise.resolve(null)
  }



  @SuppressLint("ServiceCast")
  @ReactMethod
  fun listRunningServices(promise: Promise) {
    try {
      val activityManager =
        reactApplicationContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

      val services = runningServices
        .filter { it.service.packageName == reactApplicationContext.packageName }
        .map { it.service.className }

      promise.resolve(services.toString())
    } catch (e: Exception) {
      promise.reject("ERROR", e)
    }
  }


  @RequiresApi(Build.VERSION_CODES.O)
  @Suppress("unused")
  @ReactMethod
  fun start(options: ReadableMap, triggerTime: Double, promise: Promise) {
    Thread {
      try {
         val bgOptions = BackgroundTaskOptions(this@UpsellBackgroundActionsModule.reactApplicationContext, options)
        currentServiceIntent = Intent(this@UpsellBackgroundActionsModule.reactApplicationContext, RNBackgroundActionsTask::class.java)
        println("Passed currentServiceIntent")
        runBlocking{
        println("Inside runBlocking")
        val state = StateSingleton.getInstance(this@UpsellBackgroundActionsModule.reactApplicationContext.applicationContext)
        state.setCurrentServiceIntent(currentServiceIntent!!)
        state.setBGOptions(bgOptions)
        currentServiceIntent!!.putExtras(bgOptions.extras!!)
        state.setAlarmTime(triggerTime)
          state.setIsAllTaskStoppedByUser(false)
        state.setIsBackgroundServiceRunning(true, null)
                println("Passed setIsbackgroundServiceRUNNING")
        state.setIsAllTaskStoppedByUser(false)
          state.setIsShutdown(false)
        }
        reactApplicationContext.startForegroundService(currentServiceIntent)
      } catch (e: java.lang.Exception) {
        promise.reject(e)
      }
    }.start()
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
    val singleton=StateSingleton.getInstance(reactApplicationContext)
    try {
      singleton.stopAlarmRecovery()
      singleton.stopAlarmShutdown()
      singleton.stopAlarmRestart()
      singleton.acquireRestartAlarmSemaphore()
      singleton.sendStopBroadCastRestartService()
      singleton.sendStopBroadCastAutoPrintService()
      singleton.setIsShutdown(true)
      runBlocking {
        singleton.setIsAllTaskStoppedByUser(true)
        singleton.setIsBackgroundServiceRunning(false,promise)
      }
    } catch (e: java.lang.Exception) {
      promise.reject(e)
    }finally {
        singleton.releaseRestartAlarmSemaphore()
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
      notificationManager.notify(Names().SERVICE_NOTIFICATION_ID, notification)
    } catch (e: java.lang.Exception) {
      promise.reject(e)
      return
    }
    promise.resolve(null)
  }

  @Suppress("unused")
  @ReactMethod
  fun isBackgroundServiceRunning(promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      StateSingleton.getInstance(this@UpsellBackgroundActionsModule.reactApplicationContext.applicationContext)
        .isBackgroundServiceRunning(promise)
    }
  }


  @Suppress("unused")
  @ReactMethod
  fun setIsBackgroundServiceRunning(value: Boolean, promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      println("In CourotineScope setIsBackgroundServiceRunnning")
      StateSingleton.getInstance(this@UpsellBackgroundActionsModule.reactApplicationContext.applicationContext)
        .setIsBackgroundServiceRunning(value, promise)
    }

  }

  @Suppress("unused")
  @ReactMethod
  fun sendStopBroadcast(promise: Promise) {
    try {
      val stopIntent = Intent(Names().ACTION_STOP_SERVICE)
      reactApplicationContext.sendBroadcast(stopIntent)
      promise.resolve(null)
    } catch (e: Exception) {
      promise.reject(e)

    }
  }

  @SuppressLint("MissingPermission")
  @Suppress("unused")
  @ReactMethod
  fun setAlarm(triggerTime: Double, promise: Promise) {
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
          startAlarmIntent.setAction(Names().ACTION_START_RESTART)
          val pendingIntent = PendingIntent.getBroadcast(
            reactApplicationContext,
            0,
            startAlarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )
          StateSingleton.getInstance(this.reactApplicationContext.applicationContext)
            .setPendingIntent(pendingIntent)
          this@UpsellBackgroundActionsModule.alarmPendingIntent = pendingIntent
          this@UpsellBackgroundActionsModule.alarmManager!!.setAlarmClock(
            alarmClockInfo,
            pendingIntent
          )
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
    Thread{
          StateSingleton.getInstance(reactApplicationContext.applicationContext)
        .acquireStartSemaphore(promise)
    }.start(

    )

  }

  @Suppress("unused")
  @ReactMethod
  fun interruptQueuedThread(promise: Promise) {
    CoroutineScope(Dispatchers.IO).launch {
      StateSingleton.getInstance(reactApplicationContext.applicationContext).interruptAllQueuedStartSemaphore(promise)
    }
  }

    @Suppress("unused")
    @ReactMethod
    fun unlock(promise: Promise) {
      println("In unlock release startSemaphore")
      CoroutineScope(Dispatchers.IO).launch {
        StateSingleton.getInstance(reactApplicationContext.applicationContext)
          .releaseStartSemaphore(promise)
      }
    }



    @Suppress("unused")
    @ReactMethod
    fun lockAddPrinterSemaphore(promise: Promise) {
      Thread {
        StateSingleton.getInstance(reactApplicationContext.applicationContext)
          .acquireAddPrinterSemaphore(promise)
      }.start()

    }

    @Suppress("unused")
    @ReactMethod
    fun unlockAddPrinterSemaphore(promise: Promise) {
      Thread {
        StateSingleton.getInstance(reactApplicationContext.applicationContext)
          .releaseAddPrinterSemaphore(promise)
      }.start()
    }
   @ReactMethod
   fun sendStartServiceIntentInCatch(promise:Promise){
     val startAlarmIntent = Intent(
       reactApplicationContext,
       BackgroundAlarmReceiver::class.java
     )
     startAlarmIntent.setAction(Names().ACTION_START_RESTART)
     reactApplicationContext.sendBroadcast(startAlarmIntent);
     promise.resolve(null)
   }

  @ReactMethod
  fun sendCatch(promise:Promise){
    try{
      throw Exception("Hello From Natives")
    }catch(e:Exception){
      val sentryClass = Class.forName("io.sentry.Sentry")
      val getCurrentHubMethod = sentryClass.getMethod("captureException", Throwable::class.java)
      getCurrentHubMethod.invoke(null,e)
      promise.reject(e)
    }
  }
  @ReactMethod
  fun sendMessage(promise:Promise){
    try{
      val sentryInstance= Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.captureMessage(sentryInstance,"Test Message")
        promise.resolve("Message Sent")
      }else{
        throw Exception("Sentry Not found")
      }
    }catch(e:Exception){
      promise.reject(e)
    }
  }

  @ReactMethod
  fun setOpenTimeAndCloseTime(currentTime:String,openTime:String,closeTime:String){
    try{
      val singleton=StateSingleton.getInstance(reactApplicationContext)
      val formatter= DateTimeFormatter.ofPattern("dd MM yyyy")
      val currentDateFormatted= LocalDate.now().format(formatter)
      val currentDateSplitDateTime= SplitDateTime(currentDateFormatted,currentTime)
      singleton.setOpenTme(openTime)
      singleton.setCloseTime(closeTime)
      singleton.setShutdown(currentDateSplitDateTime.toLocalDateTime(),closeTime)
    }catch(e:Exception){
      val sentry=Sentry.getSentry()
      if(sentry!==null){
        Sentry.logDebug(sentry,"Error from setOpenTimeAndCloseTime",e)
      }
    }

  }
}
