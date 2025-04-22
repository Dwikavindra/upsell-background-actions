package com.upsellbackgroundactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.runBlocking

class BackgroundAlarmReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        // Start a new thread to handle the background task
        Thread {
            try {
              if(StateSingleton.getInstance(context).acquireRestartAlarmSemaphore()) {
                val sentryInstance=Sentry.getSentry()
                if(sentryInstance!==null){
                  Sentry.captureMessage(sentryInstance,"InsideBackgroundAlarmReceiver")
                }
                println("Successfully acquired restartAlarmSemaphore")
                println("This is intent action: ${intent.action}")

                if (Names().ACTION_START_ALARM_MANAGER == intent.action) {
                  // Use runBlocking to block the thread and handle coroutines
                  var result: Boolean? = null
                  runBlocking {
                    result = StateSingleton.getInstance(context).getIsAlarmStoppedByUser()
                  }
                  if (result == true) {
                    runBlocking {
                      StateSingleton.getInstance(context).setIsBackgroundServiceRunning(false, null)
                    }
                    throw Exception("Stopped By User")
                  }

                  try {
                    runBlocking {
                      StateSingleton.getInstance(context).setisItSafeToStopAlarm(false)
                      println(
                        "This is isBackgroundServiceRunning: ${
                          StateSingleton.getInstance(
                            context
                          ).isBackgroundServiceRunning()
                        }"
                      )
                      println(
                        "Value of StateSingleton listRunningServices: ${
                          StateSingleton.getInstance(
                            context
                          ).listRunningServices()
                        }"
                      )

                      StateSingleton.getInstance(context).sendStopBroadcast()
                    }
                    Log.d("BackgroundAlarmReceiver", "Before sleep 30 seconds")
                    Thread.sleep(30000) // wait for 30 seconds to ensure that the system is off
                    Log.d("BackgroundAlarmReceiver", "After sleep 30 seconds")

                    try {
                      val currentServiceIntent =
                        Intent(context, RNBackgroundActionsTask::class.java)
                      runBlocking {
                        StateSingleton.getInstance(context)
                          .setIsBackgroundServiceRunning(false, null)
                      }

                      Log.d("BackgroundAlarmReceiver", "Passed setIsBackgroundServiceRunning false")

                      currentServiceIntent.putExtras(
                        StateSingleton.getInstance(context).getBGOptions().extras!!
                      )

                      // Acquire semaphore directly, no need for a separate thread
                      StateSingleton.getInstance(context).acquireStartSemaphore(null)
                      Log.d("BackgroundAlarmReceiver", "Passed acquireStartSemaphore")

                      runBlocking {
                        StateSingleton.getInstance(context)
                          .setIsBackgroundServiceRunning(true, null)
                      }

                      Thread.sleep(5000)
                      context.startForegroundService(currentServiceIntent)
                      Log.d("BackgroundAlarmReceiver", "Passed startService")
                      runBlocking {
                        StateSingleton.getInstance(context).setisItSafeToStopAlarm(true)
                      }
                      // After semaphore release, mark the process as safe to stop alarm

                    } catch (e: Exception) {
                      Log.d("Error in BackgroundAlarmReceiver", e.toString())
                    } finally {
                      StateSingleton.getInstance(context).releaseStartSemaphore(null)
                    }
                  } catch (e: Exception) {
                    println("Exception from background Alarm Receiver$e")
                  }
                }
              }else{
                println("Didnt pass acquireRestartAlarmSemaphore")
              }
            } catch (e: Exception) {
                println("Exception occurred in thread: ${e.toString()}")
              val sentryInstance=Sentry.getSentry()
              if(sentryInstance!==null){
                Sentry.captureException(sentryInstance,e)
              }
            } finally {
                StateSingleton.getInstance(context).releaseRestartAlarmSemaphore()
            }
        }.start() // Start the thread
    }
}
