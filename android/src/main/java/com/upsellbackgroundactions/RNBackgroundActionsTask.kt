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
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.State
import kotlin.math.floor


class RNBackgroundActionsTask : HeadlessJsTaskService() {
  private var wakeLock: PowerManager.WakeLock? = null
  private var wifiLock: WifiManager.WifiLock? = null
  private val stopServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      println("This is intent action" + intent.action)

      if (Names().ACTION_STOP_SERVICE == intent.action) {
        stopForegroundService() // Stop the foreground service when the broadcast is received
      }
    }
  }

  override fun getTaskConfig(intent: Intent): HeadlessJsTaskConfig? {
    val extras = intent.extras
    if (extras != null) {
      return HeadlessJsTaskConfig(
        extras.getString("taskName"),
        Arguments.fromBundle(extras),
        0,
        true
      )
    }
    return null
  }


  @SuppressLint("ForegroundServiceType", "UnspecifiedRegisterReceiverFlag")
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

    val extras = intent.extras
    requireNotNull(extras) { "Extras cannot be null" }
    val bgOptions = BackgroundTaskOptions(extras)
    createNotificationChannel(
      bgOptions.taskTitle,
      bgOptions.taskDesc
    ) // Necessary for creating channel for API 26+
    // Create the notification
    val notification = buildNotification(this, bgOptions)
    CoroutineScope(Dispatchers.IO).launch{
      wakeLock =
        (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
          newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RNBackgroundActionsTask::lock").apply {
            acquire(StateSingleton.getInstance(this@RNBackgroundActionsTask).getAlarmTime().toLong()+60000)
          }
        }
      wifiLock = (getSystemService(WIFI_SERVICE) as WifiManager)
        .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock")
      wifiLock!!.acquire()
    }
    val state= StateSingleton.getInstance(this)


    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(Names().SERVICE_NOTIFICATION_ID, notification,FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)

    }else{
      startForeground(Names().SERVICE_NOTIFICATION_ID,notification)
    }
    CoroutineScope(Dispatchers.IO).launch{
      state.startAlarm(state.getAlarmTime())

    }

    // Register the broadcast receiver to listen for the stop action
    val filter = IntentFilter(Names().ACTION_STOP_SERVICE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      registerReceiver(stopServiceReceiver, filter, RECEIVER_EXPORTED)
    } else {
      registerReceiver(stopServiceReceiver, filter)
    }
    super.onStartCommand(intent, flags, startId)

    return START_STICKY // Keep the service running until explicitly stopped
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
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_LOW
      val channel = NotificationChannel(Names().CHANNEL_ID, taskTitle, importance)
      channel.description = taskDesc
      val notificationManager = getSystemService(
        NotificationManager::class.java
      )
      notificationManager.createNotificationChannel(channel)
    }
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
          0,
          notificationIntent,
          PendingIntent.FLAG_IMMUTABLE
        )
      } else {
        PendingIntent.getActivity(
          context,
          0,
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
