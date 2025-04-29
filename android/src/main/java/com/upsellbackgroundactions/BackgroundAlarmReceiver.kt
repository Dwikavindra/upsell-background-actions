package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import com.facebook.react.HeadlessJsTaskService

class BackgroundAlarmReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
      HeadlessJsTaskService.acquireWakeLockNow(context)//need to acquire before going in
      if (Names().ACTION_START_RESTART == intent.action) {//restarting
          val restartTaskIntent = Intent(context, RestartTask::class.java)
          println("Received Action Start Alarm Manager")
          context.startForegroundService(restartTaskIntent)
        }
      if (Names().ACTION_SHUTDOWN == intent.action){
        val shutdownTaskIntent= Intent(context,ShutdownTask::class.java)
        context.startForegroundService(shutdownTaskIntent)

      }
      if(Names().ACTION_RECOVERY==intent.action){
        val notificationOneTime=NotificationOneTime(context)
        notificationOneTime.createNotificationChannel()
        NotificationManagerCompat.from(context).notify(Names().RECOVERY_ONE_TIME_NOTIFICATION_ID,notificationOneTime.buildNotificationRecovery())
        val restart = Intent(Names().ACTION_START_RESTART)
        context.sendBroadcast(restart)

      }
    }
}
