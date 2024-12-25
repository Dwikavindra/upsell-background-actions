package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


class StateSingleton private constructor() {
  private var functionCallBack:Callback?=null
  public var alarmTime:Double?=null
  private var reactContext:ReactContext?=null
  private var currentServiceIntent:Intent?=null
  public var isItSafeToStopAlarm:Boolean=true;
  public var isBackgroundServiceRunning=false;
  private var bgOptions:BackgroundTaskOptions?=null
  private var pendingIntent:PendingIntent?=null
  public val ACTION_STOP_SERVICE: String = "com.upsellbackgroundactions.ACTION_STOP_SERVICE"
  public val ACTION_START_ALARM_MANAGER: String = "com.upsellbackgroundactions.ACTION_START_ALARM_MANAGER"
  public val SHARED_PREFERENCES_KEY: String = "com.upsellbackgroundactions.SHARED_PREFERENCES_KEY"
  public val CHANNEL_ID = "RN_BACKGROUND_ACTIONS_CHANNEL"
  public val SERVICE_NOTIFICATION_ID: Int = 92901
  private val safeToStopAlarmSemaphore= Semaphore(1)
  private val alarmTimeSemaphore= Semaphore(1)
  private val isBackgroundServiceRunningSemaphore= Semaphore(1)
  companion object {

    @Volatile private var instance: StateSingleton? = null // Volatile modifier is necessary

    fun getInstance() =
      instance ?: synchronized(this) { // synchronized to avoid concurrency problem
        instance ?: StateSingleton().also { instance = it }
      }
  }
  fun setCallBack(callBack:Callback){
    this.functionCallBack=callBack
  }
  fun getCallBack():Callback?{
    return this.functionCallBack
  }
  fun setReactContext(context: ReactContext){
    this.reactContext=context
  }
  fun getReactContext():ReactContext{
    return this.reactContext!!
  }
  suspend fun getisItSafeToStopAlarm():Boolean {
    try {
      this.safeToStopAlarmSemaphore.acquire()
      return this.isItSafeToStopAlarm
    } catch (e: Exception) {
      Log.d("Error from getIsItSafeToStopAlarm", e.toString())
      return this.isItSafeToStopAlarm
    } finally {
      this.safeToStopAlarmSemaphore.release()
    }
  }

  suspend fun setisItSafeToStopAlarm(value:Boolean) {
    try {
      this.safeToStopAlarmSemaphore.acquire()
      this.isItSafeToStopAlarm=value
    } catch (e: Exception) {
      Log.d("Error from getIsItSafeToStopAlarm", e.toString())
    } finally {
      this.safeToStopAlarmSemaphore.release()
    }
  }
  suspend fun setAlarmTime(value: Double) {
    try {
      this.alarmTimeSemaphore.acquire()
      this.alarmTime=value
    } catch (e: Exception) {
      Log.d("Error from alarmTimeSemaphore", e.toString())
    } finally {
      this.alarmTimeSemaphore.release()
    }
  }
  suspend fun getAlarmTime():Double {
    try {
      this.alarmTimeSemaphore.acquire()
      return this.alarmTime ?: 300000.0
    } catch (e: Exception) {
      Log.d("Error from alarmTimeSemaphore", e.toString())
      return this.alarmTime ?: 300000.0
    } finally {
      this.alarmTimeSemaphore.release()
    }
  }


  fun setCurrentServiceIntent(intent: Intent){
    this.currentServiceIntent=intent
  }
  fun setBGOptions(options:BackgroundTaskOptions){
    this.bgOptions=options
  }
  fun getBGOptions():BackgroundTaskOptions{
    return this.bgOptions!!
  }
  fun setPendingIntent(pendingIntent: PendingIntent){
    synchronized(this){
      this.pendingIntent=pendingIntent
    }

  }
  @SuppressLint("MissingPermission")
  suspend fun startAlarm(triggerTime:Double,context:Context?){
    try {
      val convertedTriggerTime = triggerTime.toLong()
      this.setAlarmTime(triggerTime)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        println("inside start function Build SDK is over Lollipop")
          println("inside start function alarm manager is not null")
          val alarmClockInfo =
            AlarmClockInfo(System.currentTimeMillis() + convertedTriggerTime, null)
          val startAlarmIntent = Intent(
            context ?: this.reactContext,
            BackgroundAlarmReceiver::class.java
          )
          startAlarmIntent.setAction(StateSingleton.getInstance().ACTION_START_ALARM_MANAGER)
          val pendingIntent = PendingIntent.getBroadcast(
            context ?: this.reactContext,
            0,
            startAlarmIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
          )
          this.setPendingIntent(pendingIntent)
          this.getAlarmManager().setAlarmClock(alarmClockInfo, pendingIntent)
          println("inside start function Passed set Alarm Clock")
      } else {
        throw java.lang.Exception("OS version needs to be larger than android lollipop or android 21")
      }
    } catch (e: Exception) {
      Log.d("Error start alarm",e.toString())

    }
  }
  private fun getAlarmManager():AlarmManager{
    val alarmManager=this.reactContext!!.getSystemService(AlarmManager::class.java)
    return alarmManager
  }
  fun stopAlarm(promise: Promise){
    try{
      if(this.pendingIntent!==null){
        getAlarmManager().cancel(this.pendingIntent!!)
        promise.resolve(null)
      }else{
        throw Exception("Status of Pending Intent" + this.pendingIntent)
      }
    }catch (e:Exception){
      promise.reject(e)
    }
  }
  fun sendStopBroadcast() {
    try {
      val stopIntent= Intent(this.ACTION_STOP_SERVICE)
      this.reactContext!!.sendBroadcast(stopIntent)
    } catch (e: Exception) {
      Log.d("Error Send Stop Broadcast",e.toString())
    }
  }
  @SuppressLint("ApplySharedPref")
  suspend fun setIsBackgroundServiceRunning(value: Boolean, promise: Promise?) {
    try {
      try {
        println("Lock acquired")
        isBackgroundServiceRunningSemaphore.acquire()
        this.isBackgroundServiceRunning = value
        promise?.resolve(null)
      } finally {
        isBackgroundServiceRunningSemaphore.release()
      }
    } catch (e: InterruptedException) {
      promise?.reject("Error inSetIsBackgroundServiceRunning",e.toString())
      Thread.currentThread().interrupt()
    }
  }

  fun listRunningServices():String {
      val activityManager =
        this.reactContext!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

      val services = runningServices
        .filter { it.service.packageName == this.reactContext!!.packageName }
        .map { it.service.className }

      return services.toString()
  }
  suspend fun isBackgroundServiceRunning(promise:Promise) {
    try {
      try {
        isBackgroundServiceRunningSemaphore.acquire()
        println("Lock acquired")
        promise.resolve(this.isBackgroundServiceRunning)
      } finally {
        isBackgroundServiceRunningSemaphore.release()
      }
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
    }

  }
  fun listRunningServices(context: Context):String {
    try {
      val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

      val services = runningServices
        .filter { it.service.packageName == context.packageName }
        .map { it.service.className }

      return services.toString()
    } catch (e: Exception) {
      Log.d("Error List Running Service",e.toString())
      return "Error not able to list service"
    }
  }
////  fun waitUntilServiceisEmpty(){
////    while(this.listRunningServices()=="[]"{
//      Log.d("StateSingletonwaitUntilServiceIsEmpty")
//    }
//  }
}
