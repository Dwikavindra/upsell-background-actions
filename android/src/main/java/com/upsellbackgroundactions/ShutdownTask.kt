package com.upsellbackgroundactions

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime

class ShutdownTask: Service() {
  private fun createNotificationChannel(taskTitle: String, taskDesc: String) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(Names().CHANNEL_SHUTDOWN, taskTitle, importance)
      channel.description = taskDesc
      val notificationManager = getSystemService(
        NotificationManager::class.java
      )
      notificationManager.createNotificationChannel(channel)
    }
  }
  fun buildNotification(context: Context): Notification {
    val notificationIntent =
      Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_SHUTDOWN_ALARM_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_IMMUTABLE
      )
    } else {
      PendingIntent.getActivity(
        context,
        Names().PENDING_INTENT_SHUTDOWN_ALARM_REQUEST_CODE,
        notificationIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
      )
    }
    val builder = NotificationCompat.Builder(context,Names().CHANNEL_RESTART)
      .setContentTitle("Shutdown Autoprint at Closing Time")
      .setContentText("Shutdown Autoprint")
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentIntent(contentIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_MIN)


    return builder.build()
  }
  @SuppressLint("MissingPermission")
  fun startShutDownTask(){
    createNotificationChannel("Shutdown Autoprint at Closing Time","Shutdown Autoprint")
    buildNotification(this)
    val notificationOneTime=NotificationOneTime(this)
    notificationOneTime.createNotificationChannel()
    NotificationManagerCompat.from(this).notify(Names().SHUTDOWN_ONE_TIME_NOTIFICATION_ID,notificationOneTime.buildNotificationShutdown())

    Thread{
      val singleton=StateSingleton.getInstance(this)
      try{
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
        runBlocking {
          singleton.setIsBackgroundServiceRunning(false,null)
        }
        val restart = Intent(Names().ACTION_STOP_RESTART_SERVICE)
        this.sendBroadcast(restart)
        Thread.sleep(30000)// wait 30 seconds to kill the restart service
        val currDateTime=LocalDateTime.now()
        singleton.setShutdown(currDateTime,singleton.getCloseTime())
        singleton.setRecovery(currDateTime, singleton.getOpenTime())
      }catch (e:Exception){
        val sentry=Sentry.getSentry()
        if(sentry!==null){
          Sentry.logDebug(sentry,"Error startShutdownTask",e)
        }
      }finally {
        singleton.releaseRestartAlarmSemaphore()

      }
    }.start(
    )
  }
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    //turn off the restart
    startShutDownTask()


    return START_STICKY



  }

  override fun onBind(intent: Intent?): IBinder? {
    return null
  }
}
