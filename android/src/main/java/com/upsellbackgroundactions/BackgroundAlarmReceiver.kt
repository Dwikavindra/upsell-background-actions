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
      val singleton=StateSingleton.getInstance(context)
      HeadlessJsTaskService.acquireWakeLockNow(context)//need to acquire before going in
      if (Names().ACTION_START_RESTART == intent.action) {//restarting
          println("Received Action Start_RESTART")
          println("getIsShutdown value ${singleton.getIsShutdown()}")
          if(singleton.getIsShutdown()==false){

          val restartTaskIntent = Intent(context, RestartTask::class.java)
          println("Received Action Start Alarm Manager")
          context.startForegroundService(restartTaskIntent)
        }
        }
      if (Names().ACTION_SHUTDOWN == intent.action){
        println("Received Action Shutdown")
        val shutdownTaskIntent= Intent(context,ShutdownTask::class.java)
        context.startForegroundService(shutdownTaskIntent)

      }
      if(Names().ACTION_RECOVERY==intent.action){
        println("Received Action Recovery")
        val notificationOneTime=NotificationOneTime(context)
        notificationOneTime.createNotificationChannel()
        NotificationManagerCompat.from(context).notify(Names().RECOVERY_ONE_TIME_NOTIFICATION_ID,notificationOneTime.buildNotificationRecovery())
        val restartAlarmIntent = Intent(
          context,
          BackgroundAlarmReceiver::class.java
        )
        restartAlarmIntent.setAction(Names().ACTION_START_RESTART)
        singleton.setIsShutdown(false)
        Thread.sleep(1000)
        context.sendBroadcast(restartAlarmIntent)

      }
    }
}
