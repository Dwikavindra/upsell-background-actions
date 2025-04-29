package com.upsellbackgroundactions

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SplitDateTime(val date:String, val time:String){
  fun toLocalDateTime(): LocalDateTime {
    val formatter= DateTimeFormatter.ofPattern("dd MM yyyy HH:mm:ss", Locale.CANADA)
    val combinedDateTime="$date $time"
    val dateTime= LocalDateTime.parse(combinedDateTime, formatter)
    return dateTime
  }
}
