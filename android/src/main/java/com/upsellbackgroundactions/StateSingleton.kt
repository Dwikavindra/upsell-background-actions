package com.upsellbackgroundactions

import com.facebook.react.bridge.Callback

class StateSingleton private constructor() {

  private var functionCallBack:Callback?=null
  public val ACTION_STOP_SERVICE: String = "com.upsellbackgroundactions.ACTION_STOP_SERVICE"
  public val ACTION_START_ALARM_MANAGER: String = "com.upsellbackgroundactions.ACTION_START_ALARM_MANAGER"
  public val SHARED_PREFERENCES_KEY: String = "com.upsellbackgroundactions.SHARED_PREFERENCES_KEY"
  public val CHANNEL_ID = "RN_BACKGROUND_ACTIONS_CHANNEL"
  public val SERVICE_NOTIFICATION_ID: Int = 92901
  companion object {

    @Volatile private var instance: StateSingleton? = null // Volatile modifier is necessary

    fun getInstance() =
      instance ?: synchronized(this) { // synchronized to avoid concurrency problem
        instance ?: StateSingleton().also { instance = it }
      }
  }
  fun setCallBack(callBack:Callback){
    this.functionCallBack=callBack
  }
  fun getCallBack():Callback?{
    return this.functionCallBack
  }

}
