
import com.upsellbackgroundactions.ShutdownandRecovery
import com.upsellbackgroundactions.SplitDateTime
import org.junit.Assert.assertEquals
import org.junit.Test


class ShutdownandRecoveryTest {
  @Test
  fun `checkInBetween true case`() {
    val result = ShutdownandRecovery.isTimeInBetween("20:00:10","21:00:00", "20:30:00")
    assertEquals(true, result)
  }
  @Test
  fun `checkinBetween false case`(){
    val result = ShutdownandRecovery.isTimeInBetween("20:00:10","21:00:00", "21:30:00")
    assertEquals(false, result)
  }

  @Test
  fun `checkinBetween over false case`(){
    val result = ShutdownandRecovery.isTimeInBetween("20:00:10","21:00:00", "22:30:00")
    assertEquals(false, result)
  }


  @Test
  fun `checkinBetween under false case`(){
    val result = ShutdownandRecovery.isTimeInBetween("20:00:10","21:00:00", "19:30:00")
    assertEquals(false, result)
  }

  @Test
  fun `add1Day check for Feb 29`(){
    val result= ShutdownandRecovery.add1Day("29 02 2024")
    assertEquals("01 03 2024",result)
  }
  @Test
  fun `add1Day check for Feb 28`(){
    val result= ShutdownandRecovery.add1Day("28 02 2025")
    assertEquals("01 03 2025",result)
  }

  @Test
  fun `add1Day check for 30th`(){
    val result= ShutdownandRecovery.add1Day("30 04 2025")
    assertEquals("01 05 2025",result)
  }
  @Test
  fun `add1Day check for 31th`(){
    val result= ShutdownandRecovery.add1Day("31 03 2025")
    assertEquals("01 04 2025",result)
  }

  @Test
  fun `splitTimeToDateAndTime can seperate`(){
    val result= ShutdownandRecovery.splitTimeToDateAndTime("31 03 2025 01:00:00")
    val expectedDateTime= SplitDateTime("31 03 2025","01:00:00")
    assertEquals(expectedDateTime.date,result.date)
    assertEquals(expectedDateTime.time,result.time)
  }
  @Test
  fun `chooseDateSchedule on negative`(){
    val currentTime= SplitDateTime("31 03 2025","01:00:00").toLocalDateTime()
    val openingTime= SplitDateTime("31 03 2025","00:30:00").toLocalDateTime()
    val openingTimeTomorrow= SplitDateTime("01 04 2025","00:30:00").toLocalDateTime()
    val result = ShutdownandRecovery.chooseDateSchedule(currentTime,openingTime,openingTimeTomorrow)
    assertEquals(result,openingTimeTomorrow)
  }
  @Test
  fun `chooseDateSchedule on positive`(){
    val currentTime= SplitDateTime("31 03 2025","01:00:00").toLocalDateTime()
    val openingTime= SplitDateTime("31 03 2025","07:00:00").toLocalDateTime()
    val openingTimeTomorrow= SplitDateTime("01 04 2025","07:00:00").toLocalDateTime()
    val result = ShutdownandRecovery.chooseDateSchedule(currentTime,openingTime,openingTimeTomorrow)
    assertEquals(result,openingTime)
  }


}
