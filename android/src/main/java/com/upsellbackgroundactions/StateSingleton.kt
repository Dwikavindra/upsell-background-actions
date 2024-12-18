package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactContext
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


class StateSingleton private constructor() {
  private var functionCallBack:Callback?=null
  private var reactContext:ReactContext?=null
  private var currentServiceIntent:Intent?=null
  public var isBackgroundServiceRunning=false;
  private var bgOptions:BackgroundTaskOptions?=null
  private var pendingIntent:PendingIntent?=null
  public val ACTION_STOP_SERVICE: String = "com.upsellbackgroundactions.ACTION_STOP_SERVICE"
  public val ACTION_START_ALARM_MANAGER: String = "com.upsellbackgroundactions.ACTION_START_ALARM_MANAGER"
  public val SHARED_PREFERENCES_KEY: String = "com.upsellbackgroundactions.SHARED_PREFERENCES_KEY"
  public val CHANNEL_ID = "RN_BACKGROUND_ACTIONS_CHANNEL"
  public val SERVICE_NOTIFICATION_ID: Int = 92901
  private val lock = ReentrantLock()
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
    this.pendingIntent=pendingIntent
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
  fun setIsBackgroundServiceRunning(value: Boolean, promise: Promise?) {
    try {
      while (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
        println("Keep trying to lock")
        Thread.sleep(50) // Add a small delay between attempts
      }
      try {
        println("Lock acquired")
        this.isBackgroundServiceRunning = value
      } finally {
        lock.unlock()
      }
    } catch (e: InterruptedException) {
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
  fun isBackgroundServiceRunning(promise:Promise) {
    try {
      while (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
        println("Keep trying to lock")
        Thread.sleep(50) // Add a small delay between attempts
      }
      try {
        println("Lock acquired")
        promise.resolve(this.isBackgroundServiceRunning)
      } finally {
        lock.unlock()
      }
    } catch (e: InterruptedException) {
      Thread.currentThread().interrupt()
    }

  }
////  fun waitUntilServiceisEmpty(){
////    while(this.listRunningServices()=="[]"{
//      Log.d("StateSingletonwaitUntilServiceIsEmpty")
//    }
//  }
}
