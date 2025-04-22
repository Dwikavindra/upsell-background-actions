package com.upsellbackgroundactions

import android.util.Log

class Sentry {
  companion object{
    fun getSentry():Class<*>?{
      try{
        val sentryClass = Class.forName("io.sentry.Sentry")
        return sentryClass
      }catch (e:Exception){
       return null
      }
    }
    fun captureException(sentry:Class<*>,exception:Exception){
      try{
        val getCurrentHubMethod = sentry.getMethod("captureException", Throwable::class.java)
        getCurrentHubMethod.invoke(null,exception)
      }catch (e:Exception){
        Log.e("com.upsellbackgroundactions",e.toString())
      }
    }
    fun captureMessage(sentry:Class<*>,message:String){
      try{
        val getCurrentHubMethod = sentry.getMethod("captureMessage", String::class.java)
        getCurrentHubMethod.invoke(null,message)
      }catch (e:Exception){
        Log.e("com.upsellbackgroundactions",e.toString())
      }
    }
  }
}
