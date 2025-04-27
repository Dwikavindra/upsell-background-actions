package com.upsellbackgroundactions

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class ShutdownandRecovery {
  companion object{
    fun isTimeInBetween(openTime:String,closeTime:String, targetTime:String ):Boolean{
      val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
      val startLocalTime = LocalTime.parse(openTime, formatter)
      val endLocalTime = LocalTime.parse(closeTime, formatter)
      val checkLocalTime = LocalTime.parse(targetTime, formatter)
      var isInBetween = false
      if (endLocalTime.isAfter(startLocalTime)) {
        if (startLocalTime.isBefore(checkLocalTime) && endLocalTime.isAfter(checkLocalTime)) {
          isInBetween = true
        }
      } else if (checkLocalTime.isAfter(startLocalTime) || checkLocalTime.isBefore(endLocalTime)) {
        isInBetween = true
      }

      return isInBetween
    }
  }
}
