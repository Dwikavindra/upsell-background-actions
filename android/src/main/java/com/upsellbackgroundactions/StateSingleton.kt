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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.facebook.react.bridge.Callback
import com.facebook.react.bridge.JavaOnlyMap
import com.facebook.react.bridge.Promise
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Semaphore


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
class StateSingleton private constructor(context:Context) {
  private var functionCallBack:Callback?=null
  private var alarmTime:Double?=null
  private val context: WeakReference<Context> = WeakReference(context)
  private var currentServiceIntent:Intent?=null
  private var bgOptions:BackgroundTaskOptions?=null
  private var pendingIntent:PendingIntent?=null
  private val keyIsBackgroundServiceRunning = booleanPreferencesKey("isBackgroundServiceRunning")
  private val keyIsAlarmStoppedByUser= booleanPreferencesKey("isAlarmStoppedByUser")
  private val keyAlarmTime= doublePreferencesKey("keyAlarmTime")
  private val keyBGOptions= stringPreferencesKey("keyBGOptions")
  private val keyOpenTime= stringPreferencesKey("keyOpenTime")
  private val keyCloseTime= stringPreferencesKey("keyCloseTime")
  private val alarmStopByUserSemaphore= Semaphore(1)
  private val alarmTimeSemaphore= Semaphore(1)
  private val startSemaphore=Semaphore(1)
  private val keyIsShutdown= booleanPreferencesKey("keyIsShutdown")
  private val addPrinterSemaphore=Semaphore(1)
  private val isBackgroundServiceRunningSemaphore= Semaphore(1)
  private val restartAlarmContextSemaphore= Semaphore(1)
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


  fun acquireRestartAlarmSemaphore(){
      return restartAlarmContextSemaphore.acquire()
  }

  fun releaseRestartAlarmSemaphore(){
      restartAlarmContextSemaphore.release()

  }

  suspend fun setIsAllTaskStoppedByUser(value:Boolean) {
    try {
      this.alarmStopByUserSemaphore.acquire()
      context.get()!!.dataStore.edit { settings ->
        settings[keyIsAlarmStoppedByUser]=value
      }
    } catch (e: Exception) {
      Log.d("Error from setIsAlarmStoppedByUser", e.toString())
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error setIsAlarmStoppedByUser",e)
      }
    } finally {
      this.alarmStopByUserSemaphore.release()
    }
  }
  suspend fun getIsAllTaskStoppedByUser():Boolean {
    try {
      this.alarmStopByUserSemaphore.acquire()
      val result:Preferences= context.get()!!.dataStore.data.first()
      return result[keyIsAlarmStoppedByUser] ?: false
    } catch (e: Exception) {
      Log.d("Error from getIsAlarmStoppedByUser", e.toString())
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error getIsAlarmStoppedByUser",e)
      }
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
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error setAlarmTime",e)
      }
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
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error getAlarmTime",e)
      }
      Log.d("Error from alarmTimeSemaphore", e.toString())
      return this.alarmTime ?: 300000.0
    } finally {
      this.alarmTimeSemaphore.release()
    }
  }


  fun setCurrentServiceIntent(intent: Intent){
    this.currentServiceIntent=intent
  }
  fun getCurrentServiceIntent():Intent?{
    return this.currentServiceIntent
  }
  fun setBGOptions(options:BackgroundTaskOptions){
    this.bgOptions=options
    val map= HashMap<String,Any>()
    runBlocking {
      context.get()!!.dataStore.edit { settings ->
        map["taskTitle"] = options.taskTitle
        map["taskDesc"]=options.taskDesc
        map["iconInt"]=options.iconInt
        map["color"]=options.color
        if(options.linkingURI!=null){
          map["linkingURI"]= options.linkingURI as String
        }
        val json=Gson().toJson(map)
        settings[keyBGOptions]=json
      }
    }


  }
  fun getBGOptions():BackgroundTaskOptions?{
    try{
      if(this.bgOptions==null){
        var localBgOptions:BackgroundTaskOptions?=null
        runBlocking {
          val result:Preferences=  context.get()!!.dataStore.data.first()
          val type = object : TypeToken<HashMap<String, Any>>() {}.type
          val map=Gson().fromJson<HashMap<String,Any>>(result[keyBGOptions],type)
          val readableMap=JavaOnlyMap.from(map)
          localBgOptions=BackgroundTaskOptions(readableMap)
        }
        requireNotNull(localBgOptions)
        this.bgOptions=localBgOptions
        return this.bgOptions!!
      }else{
        return this.bgOptions!!
      }
    }catch (e:Exception) {
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error from getBgOptions",e)
      }
      Log.e("ErrorUpsellBackgroundActions",e.toString())
      return null
    }
  }
  fun setPendingIntent(pendingIntent: PendingIntent){
    synchronized(this){
      this.pendingIntent=pendingIntent
    }

  }
  fun setOpenTme(time:String){
    runBlocking {
      context.get()!!.dataStore.edit { settings ->
        settings[keyOpenTime]=time
      }
    }
  }
  fun setCloseTime(time:String){
    runBlocking {
      context.get()!!.dataStore.edit { settings ->
        settings[keyCloseTime] = time
      }
    }
  }
  fun getOpenTime():String{
    var openTime:String?=null
    runBlocking {
      val dataStore:Preferences=  context.get()!!.dataStore.data.first()
      val openTimeDataStore=dataStore[keyOpenTime]
      openTime=openTimeDataStore
    }
    requireNotNull(openTime)
    return openTime!!
  }
  fun getCloseTime():String{
    var closeTime:String?=null
    runBlocking {
      val dataStore:Preferences=  context.get()!!.dataStore.data.first()
      val openTimeDataStore=dataStore[keyCloseTime]
      closeTime=openTimeDataStore
    }
    requireNotNull(closeTime)
    return closeTime!!
  }

  @SuppressLint("MissingPermission")
  fun startAlarmShutdownTime(currentDateTime:LocalDateTime,targetDateTime: LocalDateTime){
    val currentTime: Long = currentDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val targetTime:Long=targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val alarmClockInfo = AlarmClockInfo(currentTime + (targetTime-currentTime), null)
    val startAlarmIntent = Intent(
      context.get()!!,
      BackgroundAlarmReceiver::class.java
    )
    startAlarmIntent.setAction(Names().ACTION_SHUTDOWN)
    val pendingIntent = PendingIntent.getBroadcast(
      context.get()!!,
      Names().PENDING_INTENT_SHUTDOWN_ALARM_REQUEST_CODE,
      startAlarmIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    this.getAlarmManager().setAlarmClock(alarmClockInfo, pendingIntent)
  }

  @SuppressLint("MissingPermission")
  fun startAlarmRecoveryTime(currentDateTime:LocalDateTime,targetDateTime: LocalDateTime){
    val currentTime: Long = currentDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val targetTime:Long=targetDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val alarmClockInfo = AlarmClockInfo(currentTime + (targetTime-currentTime), null)
    val startAlarmIntent = Intent(
      context.get()!!,
      BackgroundAlarmReceiver::class.java
    )
    startAlarmIntent.setAction(Names().ACTION_RECOVERY)
    val pendingIntent = PendingIntent.getBroadcast(
      context.get()!!,
      Names().PENDING_INTENT_RECOVERY_ALARM_REQUEST_CODE,
      startAlarmIntent,
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    this.getAlarmManager().setAlarmClock(alarmClockInfo, pendingIntent)
  }
  @SuppressLint("MissingPermission")
  suspend fun startRestartAlarm(triggerTime:Double){
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
          startAlarmIntent.setAction(Names().ACTION_START_RESTART)
          val pendingIntent = PendingIntent.getBroadcast(
            context.get()!!,
            Names().PENDING_INTENT_RESTART_ALARM_REQUEST_CODE,
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
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error startAlarm",e)
      }
      Log.d("Error start alarm",e.toString())

    }
  }
  fun stopAlarmRecovery(){
    try{
      val startAlarmIntent = Intent(
        this.context.get()!!,
        BackgroundAlarmReceiver::class.java
      )
      val pendingIntent = PendingIntent.getBroadcast(
        this.context.get()!!,
        Names().PENDING_INTENT_RECOVERY_ALARM_REQUEST_CODE,
        startAlarmIntent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      if(pendingIntent==null){
        throw Exception("Pending Intent not Found")
      }
      getAlarmManager().cancel(pendingIntent)
    }catch (e:Exception){
      if(e.message !="Pending Intent not Found"){

        Log.e("Error in StopAlarmRecovery",e.toString())
        val sentry= Sentry.getSentry()
        if(sentry !=null){
          Sentry.logDebug(sentry, "Error from stopAlarmRecovery",e)
        }
      }
    }
  }
  fun stopAlarmShutdown(){
    try{
      val startAlarmIntent = Intent(
        this.context.get()!!,
        BackgroundAlarmReceiver::class.java
      )
      val pendingIntent = PendingIntent.getBroadcast(
        this.context.get()!!,
        Names().PENDING_INTENT_SHUTDOWN_ALARM_REQUEST_CODE,
        startAlarmIntent,
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
      )
      if(pendingIntent==null){
        throw Exception("Pending Intent not Found")
      }
      getAlarmManager().cancel(pendingIntent)
    }catch (e:Exception){
      if(e.message!="Pending Intent not Found"){
        Log.e("Error in StopAlarmRecovery",e.toString())
        val sentry= Sentry.getSentry()
        if(sentry !=null){
          Sentry.logDebug(sentry, "Error from stopAlarmRecovery",e)
        }
      }
    }
  }
  fun setIsShutdown(value:Boolean){
    runBlocking {
      context.get()!!.dataStore.edit { settings ->
        settings[keyIsShutdown]=value
        println("Value of setting IsBackgroundServiceRunning ${settings[keyIsShutdown]}")
    }
    }
  }
  fun getIsShutdown():Boolean?{
    var isShutdown:Boolean?=null
    runBlocking {
        val result:Preferences= context.get()!!.dataStore.data.first()
        isShutdown=result[keyIsShutdown]
      }
    requireNotNull(isShutdown)
    return isShutdown
  }
  fun setShutdown(currentDateTime:LocalDateTime,closeTime:String){
    try{
      val formatter= DateTimeFormatter.ofPattern("dd MM yyyy")
      println("currentDateTime")
      val closeTimeToday= SplitDateTime(currentDateTime.format(formatter),closeTime) .toLocalDateTime()
      val closeTimeNextDay= SplitDateTime(currentDateTime.plusDays(1).format(formatter),closeTime).toLocalDateTime()
      val alarmDate=ShutdownandRecovery.chooseDateSchedule(currentDateTime,closeTimeToday,closeTimeNextDay)
      startAlarmShutdownTime(currentDateTime,alarmDate)
    }catch(e:Exception){
      val sentry= Sentry.getSentry()
      if (sentry!=null){
        Sentry.logDebug(sentry,"Error",e)
      }
      Log.e("ShutdownAndRecovery",e.toString())
    }
  }
  fun setRecovery(currentDateTime:LocalDateTime,openTime:String){
    try{
      println("This is currentDateTime on Recovery ${currentDateTime}")
      val formatter= DateTimeFormatter.ofPattern("dd MM yyyy")
      val openTimeToday= SplitDateTime(currentDateTime.format(formatter),openTime) .toLocalDateTime()
      val openTimeNextDay= SplitDateTime(currentDateTime.plusDays(1).format(formatter),openTime).toLocalDateTime()
      val alarmDate=ShutdownandRecovery.chooseDateSchedule(currentDateTime,openTimeToday,openTimeNextDay)
      println("this is alarmDate in recovery $alarmDate")
      startAlarmRecoveryTime(currentDateTime,alarmDate)
    }catch(e:Exception){
      val sentry= Sentry.getSentry()
      if (sentry!=null){
        Sentry.logDebug(sentry,"Error",e)
      }
      Log.e("ShutdownAndRecovery",e.toString())
    }
  }
  private fun getAlarmManager():AlarmManager{
    val alarmManager=this.context.get()!!.getSystemService(AlarmManager::class.java)
    return alarmManager
  }
  fun stopAlarmRestart(){
    try{
        val startAlarmIntent = Intent(
          this.context.get()!!,
          BackgroundAlarmReceiver::class.java
        )
        val pendingIntent = PendingIntent.getBroadcast(
          this.context.get()!!,
          Names().PENDING_INTENT_RESTART_ALARM_REQUEST_CODE,
          startAlarmIntent,
          PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if(pendingIntent===null){
          throw Exception("Pending Intent not Found")
        }
        getAlarmManager().cancel(pendingIntent)
    }catch (e:Exception){
      if(e.message != "Pending Intent not Found"){
        val sentryInstance=Sentry.getSentry()
        if(sentryInstance!=null){
          Sentry.logDebug(sentryInstance,"Error stopAlarmInsideService",e)
        }
      }
    }
  }
  fun sendStopBroadCastAutoPrintService() {
    try {
      val stopIntent= Intent(Names().ACTION_STOP_SERVICE)
      this.context.get()!!.sendBroadcast(stopIntent)
    } catch (e: Exception) {
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error sendStopBroadcast",e)
      }
      Log.d("Error Send Stop Broadcast",e.toString())
    }
  }
  fun sendStopBroadCastRestartService() {
    try {
      val stopIntent= Intent(Names().ACTION_STOP_RESTART_SERVICE)
      this.context.get()!!.sendBroadcast(stopIntent)
    } catch (e: Exception) {
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error sendStopBroadcast",e)
      }
      Log.d("Error Send Stop Broadcast",e.toString())
    }
  }
  @SuppressLint("ApplySharedPref")
  suspend fun setIsBackgroundServiceRunning(value: Boolean, promise: Promise?) {
    try {
      isBackgroundServiceRunningSemaphore.acquire()
      println("Here in setIs backgroundServiceRunning ")
      context.get()!!.dataStore.edit { settings ->
        settings[keyIsBackgroundServiceRunning]=value
        println("Value of setting IsBackgroundServiceRunning ${settings[keyIsBackgroundServiceRunning]}")
        promise?.resolve(null)
      }

    } catch (e:Exception) {
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error setIsBackgroundServiceRunning",e)
      }
      promise?.reject("Error inSetIsBackgroundServiceRunning",e.toString())
    }finally {
        isBackgroundServiceRunningSemaphore.release()
    }
  }

  suspend fun isBackgroundServiceRunning(promise:Promise?=null):Boolean {
    try {
        isBackgroundServiceRunningSemaphore.acquire()
        println("Lock acquired isBackgroundServiceRunningSemaphore")
        val result:Preferences=  context.get()!!.dataStore.data.first()
        println("Value of isBackgroundServiceRunning ${result[keyIsBackgroundServiceRunning]}")
        promise?.resolve(result[keyIsBackgroundServiceRunning] ?: false)
        return  result[keyIsBackgroundServiceRunning] ?: false
      } catch (e: Exception) {
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error isBackgroundServiceRunning",e)
      }
        promise?.reject("Error from isBackgroundServiceRunning",e)
        return false
    } finally {
      isBackgroundServiceRunningSemaphore.release()
    }
  }
  fun acquireStartSemaphore(promise:Promise?){

      try{
        startSemaphore.acquire()
        promise?.resolve(null)
      }catch (e:InterruptedException){
        val sentryInstance=Sentry.getSentry()
        if(sentryInstance!==null){
          Sentry.logDebug(sentryInstance,"Error acquireStartSemaphore",e)
        }
        promise?.reject(e)
      }


  }

 fun  releaseStartSemaphore(promise:Promise?){
   try{
     if(startSemaphore.availablePermits()==0){
       startSemaphore.release()
       promise?.resolve(null)
     }
     promise?.resolve("Not Safe to release")

   }catch(e:Exception){
     val sentryInstance=Sentry.getSentry()
     if(sentryInstance!==null){
       Sentry.logDebug(sentryInstance,"Error releaseStartSemaphore",e)
     }
     promise?.reject(e)
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
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"Error interruptAllQueuedStartSemaphore",e)
      }
      promise.reject(e)
    }

  }

   fun acquireAddPrinterSemaphore(promise:Promise?){
      addPrinterSemaphore.acquire()
      promise?.resolve(null)


  }

  fun  releaseAddPrinterSemaphore(promise:Promise?){
    addPrinterSemaphore.release()
    promise?.resolve(null)
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
