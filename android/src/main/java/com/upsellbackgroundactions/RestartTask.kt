package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.runBlocking
import kotlin.math.floor

class RestartTask: Service() {

/*
Restart would not be accurate with an addition 30 seconds to wait for the service to turn off with sendStopBroadcast
* */
  private fun createNotificationChannel(taskTitle: String, taskDesc: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_LOW
      val channel = NotificationChannel(Names().CHANNEL_RESTART, taskTitle, importance)
      channel.description = taskDesc
      val notificationManager = getSystemService(
        NotificationManager::class.java
      )
      notificationManager.createNotificationChannel(channel)
    }
  }
  fun setupNotification():Notification{
    createNotificationChannel("Restart Autoprint","autoprint restart")
    val singleton = StateSingleton.getInstance(this)
    return buildNotification(this)
  }

  @SuppressLint("NewApi")
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    executeRestartTask()
    return START_STICKY // Keep the service running until explicitly stopped
  }
  private fun stopForegroundService() {
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }
  @RequiresApi(Build.VERSION_CODES.O)
  fun executeRestartTask() {
    Thread {
      val sentryInstance=Sentry.getSentry()
      val singleton = StateSingleton.getInstance(this)
      try {
        val notification = setupNotification()
        ServiceCompat.startForeground(this,Names().SERVICE_NOTIFICATION_ID, notification,
          FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        if (!singleton.acquireRestartAlarmSemaphore()) {
          println("Didn't pass acquireRestartAlarmSemaphore")
          return@Thread
        }
        println("Successfully acquired restartAlarmSemaphore")
        if (isAlarmStoppedByUser()) {
          return@Thread
        }

        performRestartSequence(singleton)
      } catch (e: Exception) {
        Log.e("acquireRestartSemaphore",e.toString())
        if(sentryInstance!==null){
          Sentry.logDebug(sentryInstance,"From Restart Task acquireRestartAlarmSemaphore",e)
        }
      } finally {
        singleton.releaseRestartAlarmSemaphore()
        stopForegroundService()
      }
    }.start()
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun performRestartSequence(singleton: StateSingleton) {
    try {
      stopCurrentService()
      waitForSystemShutdown()
      startNewService(singleton)
    } catch (e: Exception) {
      val sentryInstance=Sentry.getSentry()
      println("Exception from background Alarm Receiver$e")
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"From Restart Task perfromRestartSequence",e)
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  private fun startNewService(singleton: StateSingleton) {
    try {
      val currentServiceIntent = Intent(this, RNBackgroundActionsTask::class.java)


      Log.d("BackgroundAlarmReceiver", "Passed setIsBackgroundServiceRunning false")
      currentServiceIntent.putExtras(singleton.getBGOptions()!!.extras!!)
      singleton.acquireStartSemaphore(null)
      Log.d("BackgroundAlarmReceiver", "Passed acquireStartSemaphore")
      runBlocking {
        StateSingleton.getInstance(this@RestartTask).setIsBackgroundServiceRunning(true, null)
      }
      Thread.sleep(5000)
      this.startForegroundService(currentServiceIntent)
      Log.d("BackgroundAlarmReceiver", "Passed startService")
      runBlocking {
        StateSingleton.getInstance(this@RestartTask).setisItSafeToStopAlarm(true)
      }
    } catch (e: Exception) {
      val sentryInstance=Sentry.getSentry()
      if(sentryInstance!==null){
        Sentry.logDebug(sentryInstance,"From startNewService",e)
      }
      Log.d("Error in BackgroundAlarmReceiver", e.toString())
    } finally {
      singleton.releaseStartSemaphore(null)
    }
  }


  private fun waitForSystemShutdown() {
    Log.d("BackgroundAlarmReceiver", "Before sleep 30 seconds")

    Thread.sleep(30000) // wait for 30 seconds to ensure that the system is off
    Log.d("BackgroundAlarmReceiver", "After sleep 30 seconds")
    val services=StateSingleton.getInstance(this@RestartTask).listRunningServices()
    Log.d("BackgroundAlarmListService", services)
  }

  private fun stopCurrentService() {
    runBlocking {
      StateSingleton.getInstance(this@RestartTask).setisItSafeToStopAlarm(false)
      StateSingleton.getInstance(this@RestartTask).setIsBackgroundServiceRunning(false, null)
      StateSingleton.getInstance(this@RestartTask).sendStopBroadcast()
    }
  }
  private fun isAlarmStoppedByUser(): Boolean {
    var stoppedByUser: Boolean? = null
    runBlocking {
      stoppedByUser = StateSingleton.getInstance(this@RestartTask).getIsAlarmStoppedByUser()
    }

    if (stoppedByUser == true) {
      runBlocking {
        StateSingleton.getInstance(this@RestartTask).setIsBackgroundServiceRunning(false, null)
      }
      return true
    }
    return false
  }
  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
    fun buildNotification(context: Context): Notification {
    val notificationIntent =
      Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.getActivity(
        context,
        1,
        notificationIntent,
        PendingIntent.FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        context,
        1,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }
    val builder = NotificationCompat.Builder(context,Names().CHANNEL_RESTART)
      .setContentTitle("Restart Autoprint")
      .setContentText("autoprint restart")
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentIntent(contentIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_MIN)


    return builder.build()
  }
}









