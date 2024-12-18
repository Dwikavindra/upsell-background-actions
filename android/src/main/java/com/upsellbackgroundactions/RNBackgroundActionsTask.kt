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
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig
import kotlin.math.floor


class RNBackgroundActionsTask : HeadlessJsTaskService() {

  private val stopServiceReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      println("This is intent action" + intent.action)

      if (StateSingleton.getInstance().ACTION_STOP_SERVICE == intent.action) {
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

    startForeground(StateSingleton.getInstance().SERVICE_NOTIFICATION_ID, notification)

    // Register the broadcast receiver to listen for the stop action
    val filter = IntentFilter(StateSingleton.getInstance().ACTION_STOP_SERVICE)
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

    stopForeground(true)
    stopSelf()

  println("Passed statement stopForeground and stopSelf")
    unregisterReceiver(stopServiceReceiver) // Unregister the broadcast receiver
  }

  private fun stopForegroundService() {
    println("On Stop Foreground Service before stopForeground")
    stopForeground(true) // Stop the foreground service and remove the notification
    stopSelf() // Stop the service itself
    println("Passed stopSelf")

  }

  private fun createNotificationChannel(taskTitle: String, taskDesc: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_LOW
      val channel = NotificationChannel(StateSingleton.getInstance().CHANNEL_ID, taskTitle, importance)
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
      val builder = NotificationCompat.Builder(context,StateSingleton.getInstance().CHANNEL_ID)
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
