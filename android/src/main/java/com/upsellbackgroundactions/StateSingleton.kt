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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.Promise
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.first



import java.lang.ref.WeakReference
import java.util.concurrent.Semaphore


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class StateSingleton private constructor(context:Context) {
  private var functionCallBack:Callback?=null
  public var alarmTime:Double?=null
  private val context: WeakReference<Context> = WeakReference(context)
  private var currentServiceIntent:Intent?=null
  private var bgOptions:BackgroundTaskOptions?=null
  private var pendingIntent:PendingIntent?=null
  private val keyIsBackgroundServiceRunning = booleanPreferencesKey("isBackgroundServiceRunning")
  private val keyIsItSafeToStopAlarm= booleanPreferencesKey("isItSafeToStopAlarm")
  private val keyIsAlarmStoppedByUser= booleanPreferencesKey("isAlarmStoppedByUser")
  private val keyAlarmTime= doublePreferencesKey("keyAlarmTime")
  private val safeToStopAlarmSemaphore= Semaphore(1)
  private val alarmStopByUserSemaphore= Semaphore(1)
  private val alarmTimeSemaphore= Semaphore(1)
  private val startSemaphore=Semaphore(1)
  private val addPrinterSemaphore=Semaphore(1)
  private val isBackgroundServiceRunningSemaphore= Semaphore(1)
  private val alarmContextSemaphore= Semaphore(1)
  private val startSemaphoreRelease=Semaphore(1)
  companion object {


    @Volatile private var instance: StateSingleton? = null // Volatile modifier is necessary

    fun getInstance(context:Context):StateSingleton {
     return instance ?: synchronized(this) { // synchronized to avoid concurrency problem
        instance ?: StateSingleton(context).also { instance = it }
      }
    }
    }
  fun setCallBack(callBack:Callback){
    this.functionCallBack=callBack
  }
  fun getCallBack():Callback?{
    return this.functionCallBack
  }

  suspend fun getisItSafeToStopAlarm():Boolean {
    try {
      this.safeToStopAlarmSemaphore.acquire()
      val result:Preferences= context.get()!!.dataStore.data.first()
      return result[keyIsItSafeToStopAlarm] ?: false
    } catch (e: Exception) {
      Log.d("Error from getIsItSafeToStopAlarm", e.toString())
      return true// return default value
    } finally {
      this.safeToStopAlarmSemaphore.release()
    }
  }

  suspend fun getContext():Context{
    return this.context.get()!!
  }
  @Suppress("withContext")
  suspend fun setisItSafeToStopAlarm(value:Boolean) {
    try {
      this.safeToStopAlarmSemaphore.acquire()
      context.get()!!.dataStore.edit { settings ->
        settings[keyIsItSafeToStopAlarm]=value
      }
    } catch (e: Exception) {
      Log.d("Error from getIsItSafeToStopAlarm", e.toString())
    } finally {
      this.safeToStopAlarmSemaphore.release()
    }
  }


  suspend fun setIsAlarmStoppedByUser(value:Boolean) {
    try {
      this.alarmStopByUserSemaphore.acquire()
      context.get()!!.dataStore.edit { settings ->
        settings[keyIsAlarmStoppedByUser]=value
      }
    } catch (e: Exception) {
      Log.d("Error from setIsAlarmStoppedByUser", e.toString())
    } finally {
      this.alarmStopByUserSemaphore.release()
    }
  }
  suspend fun getIsAlarmStoppedByUser():Boolean {
    try {
      this.alarmStopByUserSemaphore.acquire()
      val result:Preferences= context.get()!!.dataStore.data.first()
      return result[keyIsAlarmStoppedByUser] ?: false
    } catch (e: Exception) {
      Log.d("Error from getIsAlarmStoppedByUser", e.toString())
      return true
    } finally {
      this.alarmStopByUserSemaphore.release()
    }
  }
  suspend fun setAlarmTime(value: Double) {
    try {
      this.alarmTimeSemaphore.acquire()
      context.get()!!.dataStore.edit { settings ->
        settings[keyAlarmTime]=value
        println("Value of setting IsBackgroundServiceRunning ${settings[keyIsBackgroundServiceRunning]}")
      }
    } catch (e: Exception) {
      Log.d("Error from alarmTimeSemaphore", e.toString())
    } finally {
      this.alarmTimeSemaphore.release()
    }
  }
  suspend fun getAlarmTime():Double {
    try {
      this.alarmTimeSemaphore.acquire()
      val result:Preferences= context.get()!!.dataStore.data.first()
      return result[keyAlarmTime] ?: 300000.0
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
  suspend fun startAlarm(triggerTime:Double){
    try {
      val convertedTriggerTime = triggerTime.toLong()
      this.setAlarmTime(triggerTime)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        println("inside start function Build SDK is over Lollipop")
          println("inside start function alarm manager is not null")
          val alarmClockInfo =
            AlarmClockInfo(System.currentTimeMillis() + convertedTriggerTime, null)
          val startAlarmIntent = Intent(
            context.get()!!,
            BackgroundAlarmReceiver::class.java
          )
          startAlarmIntent.setAction(Names().ACTION_START_ALARM_MANAGER)
          val pendingIntent = PendingIntent.getBroadcast(
            context.get()!!,
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
    val alarmManager=this.context.get()!!.getSystemService(AlarmManager::class.java)
    return alarmManager
  }
  suspend fun stopAlarm(context:Context?, promise: Promise){
    try{
      this.setIsAlarmStoppedByUser(true)
      if(getisItSafeToStopAlarm()){
        val startAlarmIntent = Intent(
          this.context.get()!!,
          BackgroundAlarmReceiver::class.java
        )
        val pendingIntent = PendingIntent.getBroadcast(
          this.context.get()!!,
          0,
          startAlarmIntent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if(pendingIntent===null){
          throw Exception("Pending Intent not Found")
        }
        getAlarmManager().cancel(pendingIntent)

        promise.resolve(null)
      }else{
        throw Exception("Not Safe to stop Alarm")
      }


    }catch (e:Exception){
      promise.reject(e)
    }
  }
  fun sendStopBroadcast() {
    try {
      val stopIntent= Intent(Names().ACTION_STOP_SERVICE)
      this.context.get()!!.sendBroadcast(stopIntent)
    } catch (e: Exception) {
      Log.d("Error Send Stop Broadcast",e.toString())
    }
  }
  @SuppressLint("ApplySharedPref")
  suspend fun setIsBackgroundServiceRunning(value: Boolean, promise: Promise?) {
    try {
      println("Here in setIs backgroundServiceRunning ")
      context.get()!!.dataStore.edit { settings ->
        settings[keyIsBackgroundServiceRunning]=value
        println("Value of setting IsBackgroundServiceRunning ${settings[keyIsBackgroundServiceRunning]}")
        promise?.resolve(null)
      }

    } catch (e:Exception) {
      println("setIsbackgroundServicerunning exception")
      promise?.reject("Error inSetIsBackgroundServiceRunning",e.toString())
    }
  }

  suspend fun isBackgroundServiceRunning(promise:Promise?=null):Boolean {
    try {
        isBackgroundServiceRunningSemaphore.acquire()
        println("Lock acquired isBackgroundServiceRunningSemaphore")
        val result:Preferences=  context.get()!!.dataStore.data.first()
      println("Value of preference ")
        promise?.resolve(result[keyIsBackgroundServiceRunning] ?: false)
        println("This is value of isBackgroundServiceRunning ${result[keyIsBackgroundServiceRunning]}")
        return  result[keyIsBackgroundServiceRunning] ?: false
      } catch (e: Exception) {
        promise?.reject("Error from isBackgroundServiceRunning",e)
        return false
    } finally {
      isBackgroundServiceRunningSemaphore.release()
    }
  }
  suspend fun acquireStartSemaphore(promise:Promise){

      try{
        startSemaphore.acquire()
        promise.resolve(null)
      }catch (e:InterruptedException){
        promise.reject(e)
      }


  }

 suspend fun  releaseStartSemaphore(promise:Promise){
   println("In release startSemaphore")
   try{
     println("This is available permits ${startSemaphoreRelease.availablePermits()}")
     if(startSemaphore.availablePermits()==0){
       startSemaphore.release()
       promise.resolve(null)
     }
     promise.resolve("Not Safe to release")

   }catch(e:Exception){
     promise.reject(e)
   }
  }
  @Suppress("UNCHECKED_CAST")
  fun interruptAllQueuedStartSemaphore(promise:Promise){
    try{
      val method= Semaphore::class.java.getDeclaredMethod("getQueuedThreads")
      method.isAccessible = true // Bypass access control checks
      val result : Collection<Thread> = method.invoke(startSemaphore) as Collection<Thread>
      for (thread in result) {
        thread.interrupt()
      }
      promise.resolve(null)
    }catch (e:Exception){
      promise.reject(e)
    }

  }

  suspend fun acquireAddPrinterSemaphore(promise:Promise){
      addPrinterSemaphore.acquire()
      promise.resolve(null)


  }
  fun  releaseAddPrinterSemaphore(promise:Promise){
    addPrinterSemaphore.release()
    promise.resolve(null)
  }

  fun listRunningServices():String {
    try {
      val activityManager = context.get()!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
      val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

      val services = runningServices
        .filter { it.service.packageName == context.get()!!.packageName }
        .map { it.service.className }

      return services.toString()
    } catch (e: Exception) {
      Log.d("Error List Running Service",e.toString())
      return "Error not able to list service"
    }
  }
}
