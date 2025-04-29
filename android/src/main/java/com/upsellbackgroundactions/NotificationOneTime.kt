package com.upsellbackgroundactions
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime


class NotificationOneTime (val context: Context){
    fun createNotificationChannel(){// should just be called one time
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = Names()
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(name.CHANNEL_ONE_TIME_ID, name.CHANNEL_ONE_TIME, importance).apply {
          description = "One Time Notification Channel"
        }
        val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
      }
    }

  fun buildNotificationShutdown(): Notification {
    val notificationIntent =
      Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_SHUTDOWN_ONE_TIME_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_SHUTDOWN_ONE_TIME_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }
    val builder = NotificationCompat.Builder(context,Names().CHANNEL_ONE_TIME_ID)
      .setContentTitle("Shutdown AutoPrint at Closing Time")
      .setContentText("Time ${LocalDateTime.now()}")
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentIntent(contentIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_MIN)


    return builder.build()
  }

  fun buildNotificationRecovery(): Notification {
    val notificationIntent =
      Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_RECOVERY_ONE_TIME_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_RECOVERY_ONE_TIME_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }
    val builder = NotificationCompat.Builder(context,Names().CHANNEL_ONE_TIME_ID)
      .setContentTitle("Recover AutoPrint at Closing Time")
      .setContentText("Time ${LocalDateTime.now()}")
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentIntent(contentIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_MIN)


    return builder.build()
  }



}
