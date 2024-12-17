package com.upsellbackgroundactions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class BackgroundAlarmReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    println("This is intent action" + intent.action)
    if (StateSingleton.getInstance().ACTION_START_ALARM_MANAGER == intent.action) {
      try {
        StateSingleton.getInstance().getCallBack()?.invoke("Restart callBack Invoked") ?: run {
          Log.d("UpsellBackgroundModule","Callback function not found")
          //shut down the process natively
        }
        println("Start success in BackgroundAlarmReciever")
      } catch (e: Exception) {
        println("Exception from background Alarm Receiver$e")
      }
      //            currentServiceIntent.putExtras(bgOptions.getExtras());
//            context.startService(currentServiceIntent);
    }
  }
}
