package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import kotlinx.coroutines.runBlocking
import kotlin.math.floor


class RNBackgroundActionsTask : HeadlessJsTaskService() {
  private var wakeLock: PowerManager.WakeLock? = null
  private var wifiLock: WifiManager.WifiLock? = null
  private val stopServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      if (Names().ACTION_STOP_SERVICE == intent.action) {
        stopForegroundService() // Stop the foreground service when the broadcast is received
      }
    }
  }



  @RequiresApi(Build.VERSION_CODES.TIRAMISU)
  @SuppressLint("ForegroundServiceType", "UnspecifiedRegisterReceiverFlag")
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    try {
      val singleton = StateSingleton.getInstance(this)
      val bgOptions= singleton.getBGOptions()!!.extras!!
      val config= HeadlessJsTaskConfig(
        bgOptions.getString("taskName") ?:"AutoPrint",// it must exist
        Arguments.fromBundle(bgOptions),
        0,
        true
      )
      startTask(config)
      threadPromoteToForegroundSetAlarm(singleton)
      return START_STICKY // Keep the service running until explicitly stopped
    }catch (e:Exception){
      val sentryInstance = Sentry.getSentry()
      if (sentryInstance !== null) {
        Sentry.logDebug(sentryInstance, "onStartCommand RNBackgroundActionTask FatalError Service is Stopped", e)
      }
      stopForegroundService()
      return START_NOT_STICKY
    }


  }

  private fun threadPromoteToForegroundSetAlarm(singleton: StateSingleton) {
    Thread {
      try {
        val bgOptions = BackgroundTaskOptions(singleton.getBGOptions()!!.extras!!)
        createNotificationChannel(
          bgOptions.taskTitle,
          bgOptions.taskDesc
        )
        val notification = buildNotification(this, bgOptions)
        runBlocking {
          wakeLock =
            (getSystemService(POWER_SERVICE) as PowerManager).run {
              newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RNBackgroundActionsTask::lock").apply {
                acquire(
                  StateSingleton.getInstance(this@RNBackgroundActionsTask).getAlarmTime()
                    .toLong() + 240000
                )
              }
            }
          wifiLock = (getSystemService(WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock")
          wifiLock!!.acquire()
        }


        val state = StateSingleton.getInstance(this)


        ServiceCompat.startForeground(
          this, Names().SERVICE_NOTIFICATION_ID, notification,
          FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )


        runBlocking {
          state.stopAlarmRestart()//ensure only one alarm instance
          println("This is state.getAlarmTime ${state.getAlarmTime()}")
          state.startRestartAlarm(state.getAlarmTime())
        }
        val filter = IntentFilter(Names().ACTION_STOP_SERVICE)
        registerReceiver(stopServiceReceiver, filter, RECEIVER_EXPORTED)
      } catch (e: Exception) {
        val sentryInstance = Sentry.getSentry()
        if (sentryInstance !== null) {
          Sentry.logDebug(
            sentryInstance,
            "onStartCommand RNBackgroundActionTask Error in Thread",
            e
          )
        }
      }

    }.start()
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  private fun stopForegroundService() {

    wakeLock?.let {
      if (it.isHeld) {
        it.release()
      }
    }// release wakeLock
    wifiLock?.let {
      if (it.isHeld) {
        it.release()
      }
    }// release wakeLock
    println("On Stop Foreground Service before stopForeground")
    stopForeground(STOP_FOREGROUND_REMOVE) // Stop the foreground service and remove the notification
    stopSelf() // Stop the service itself
    println("Passed stopSelf")
    unregisterReceiver(stopServiceReceiver) // Unregister the broadcast receiver
  }

  private fun createNotificationChannel(taskTitle: String, taskDesc: String) {
      val importance = NotificationManager.IMPORTANCE_LOW
      val channel = NotificationChannel(Names().CHANNEL_ID, taskTitle, importance)
      channel.description = taskDesc
      val notificationManager = getSystemService(
        NotificationManager::class.java
      )
      notificationManager.createNotificationChannel(channel)
  }

  companion object {

    @SuppressLint("UnspecifiedImmutableFlag")
    fun buildNotification(context: Context, bgOptions: BackgroundTaskOptions): Notification {

      // Get info
      val taskTitle = bgOptions.taskTitle
      val taskDesc = bgOptions.taskDesc
      val iconInt = bgOptions.iconInt
      val color = bgOptions.color
      val linkingURI = bgOptions.linkingURI
      val notificationIntent = if (linkingURI != null) {
        Intent(Intent.ACTION_VIEW, Uri.parse(linkingURI))
      } else {
        // As RN works on single activity architecture - we don't need to find current activity on behalf of react context
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
      }
      val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(
          context,
          Names().PENDING_INTENT_AUTOPRINT_NOTIFICATION_REQUEST_CODE,
          notificationIntent,
          PendingIntent.FLAG_IMMUTABLE
        )
      } else {
        PendingIntent.getActivity(
          context,
          Names().PENDING_INTENT_AUTOPRINT_NOTIFICATION_REQUEST_CODE,
          notificationIntent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
      }
      val builder = NotificationCompat.Builder(context,Names().CHANNEL_ID)
        .setContentTitle(taskTitle)
        .setContentText(taskDesc)
        .setSmallIcon(iconInt)
        .setContentIntent(contentIntent)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setColor(color)

      val progressBarBundle = bgOptions.progressBar
      if (progressBarBundle != null) {
        val progressMax = floor(progressBarBundle.getDouble("max")) as Int
        val progressCurrent = floor(progressBarBundle.getDouble("value")) as Int
        val progressIndeterminate = progressBarBundle.getBoolean("indeterminate")
        builder.setProgress(progressMax, progressCurrent, progressIndeterminate)
      }
      return builder.build()
    }
  }
}
