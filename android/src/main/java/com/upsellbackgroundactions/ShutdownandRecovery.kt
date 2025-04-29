package com.upsellbackgroundactions

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


class ShutdownandRecovery {
  companion object{
    fun isTimeInBetween(biggerThan:String, lessThan:String, targetTime:String ):Boolean{
      val formatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
      val startLocalTime = LocalTime.parse(biggerThan, formatter)
      val endLocalTime = LocalTime.parse(lessThan, formatter)
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
    fun add1Day(currentDate:String): String{
      val formatter= DateTimeFormatter.ofPattern("dd MM yyyy",Locale.CANADA)
      val parseCurrentDate= LocalDate.parse(currentDate,formatter)
      val result=parseCurrentDate.plusDays(1).format(formatter)
      return result
    }

    fun splitTimeToDateAndTime(currentTime:String):SplitDateTime{
      val formatter= DateTimeFormatter.ofPattern("dd MM yyyy HH:mm:ss",Locale.CANADA)
      val formatterDate= DateTimeFormatter.ofPattern("dd MM yyyy",Locale.CANADA)
      val formatterHour = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US)
      val dateTime= LocalDateTime.parse(currentTime, formatter)
      val date=dateTime.toLocalDate().format(formatterDate)
      val time=dateTime.toLocalTime().format(formatterHour)
      val dataClass= SplitDateTime(date,time)
      return dataClass
    }

    /**
     * chooseDateSchedule: choose the next closest alarm date
     * Problem when choosing the next alarm schedule what happens if the close schedule is 1AM but opening is 2AM, how do you determine the next schedule of close from opening it's tomorrow right?
     * Imagine you are at 2AM you need to schedule to tomorrow of course just add 1 day into it and it's fine
     * but say you are opening at 00:30 AM would it be tomorrow? No (of course this hypothetical and no one does that but how would you do it such that you make the algo bulletproof)
     * You do it by calculating the difference between targetTime (closing or opening time) - currenTime
     * you do it to currentTime and the next day , so test it against 2 targetTime+1 day - currentTime and targetTime(today)-currentTime
     * Choose the lesser one but if it's negative choose the next day
     */
    fun chooseDateSchedule(currentDate:LocalDateTime, targetTimeToday:LocalDateTime, targetTimeNextDay:LocalDateTime):LocalDateTime{
      val currentDateMillis= ZonedDateTime.of(currentDate,ZoneId.systemDefault()).toInstant().toEpochMilli()
      val targetTimeTodayMillis= ZonedDateTime.of(targetTimeToday,ZoneId.systemDefault()).toInstant().toEpochMilli()
      val targetTimeNextDayMillis= ZonedDateTime.of(targetTimeNextDay,ZoneId.systemDefault()).toInstant().toEpochMilli()
      val differenceWithTargetTimeToday= targetTimeTodayMillis-currentDateMillis
      val differenceWithTargetTimeTomorrow= targetTimeNextDayMillis-currentDateMillis
      if(differenceWithTargetTimeToday<0){
        return targetTimeNextDay
      }else{
        if(differenceWithTargetTimeToday<differenceWithTargetTimeTomorrow){
          return targetTimeToday
        }else{
          return targetTimeNextDay
        }
      }
    }
  }
}
