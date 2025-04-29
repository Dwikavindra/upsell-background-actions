package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.runBlocking

class RestartTask: Service() {
  private val stopServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (Names().ACTION_STOP_RESTART_SERVICE == intent.action) {
        stopForegroundService() // Stop the foreground service when the broadcast is received
      }
    }
  }

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
        singleton.acquireRestartAlarmSemaphore()
        var stopThread=false
        runBlocking {
          if(singleton.getIsAllTaskStoppedByUser()){
            stopThread=true
          }
        }
        if(stopThread){
          return@Thread
        }

        performRestartSequence(singleton)
      } catch (e: Exception) {
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
      StateSingleton.getInstance(this@RestartTask).setIsBackgroundServiceRunning(false, null)
      StateSingleton.getInstance(this@RestartTask).sendStopBroadCastAutoPrintService()
    }
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
        Names().PENDING_INTENT_RESTART_NOTIFICATION_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_RESTART_NOTIFICATION_REQUEST_CODE,
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









