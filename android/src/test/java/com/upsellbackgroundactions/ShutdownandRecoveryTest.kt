
import com.upsellbackgroundactions.ShutdownandRecovery
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

}
