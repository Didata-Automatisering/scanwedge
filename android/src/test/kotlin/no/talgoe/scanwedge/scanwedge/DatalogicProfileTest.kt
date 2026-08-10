package no.talgoe.scanwedge.scanwedge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/*
* DataLogic profiles are a comma separated `PROPERTY=value` map sent to
* com.datalogic.device.intent.action.configuration.COMMIT. A property name DataLogic does not know
* fails the whole commit, which is silent: the scanner keeps the previously applied profile, so the
* caller gets no scans for the barcodes it enabled. These tests pin the property names we emit.
*
* Run with `./gradlew testDebugUnitTest` in `example/android/`.
*/
internal class DatalogicProfileTest {
  private val log = TestLogger()
  private fun properties(vararg barcodes: BarcodePlugin): List<String> {
    val lst = ArrayList<String>()
    barcodes.forEach { it.datalogicAddToList(lst) }
    return lst
  }

  @Test
  fun code128_getsLengthControl() {
    assertEquals(
      listOf("CODE128_ENABLE=true", "CODE128_LENGTH_CONTROL=3", "CODE128_LENGTH1=20", "CODE128_LENGTH2=20"),
      properties(BarcodePlugin(BarcodeTypes.CODE128, 20, 20, log)),
    )
  }

  @Test
  fun gs1_128_isEnabledWithoutLengthProperties() {
    // CODE128_GS1 is an option of the Code 128 decoder: it has no CODE128_GS1_LENGTH* properties, and
    // sending them made the whole profile fail to apply on a Memor 17.
    val props = properties(BarcodePlugin(BarcodeTypes.EAN128, 20, 20, log))
    assertEquals(listOf("CODE128_GS1_ENABLE=true"), props)
    assertFalse(props.any { it.contains("CODE128_GS1_LENGTH") })
  }

  @Test
  fun fixedLengthSymbologiesGetNoLengthProperties() {
    for (type in listOf(BarcodeTypes.EAN13, BarcodeTypes.EAN8, BarcodeTypes.UPCA, BarcodeTypes.UPCE0, BarcodeTypes.GS1_DATABAR, BarcodeTypes.MAXICODE)) {
      val props = properties(BarcodePlugin(type, 1, 30, log))
      assertEquals(1, props.size, "$type should only be enabled/disabled, got $props")
      assertTrue(props.first().endsWith("_ENABLE=true"), "$type: ${props.first()}")
    }
  }

  @Test
  fun unrestrictedLengthEmitsNoLengthControl() {
    // No min/max means "leave it alone" - DataLogic then keeps whatever length control was set last.
    assertEquals(listOf("QRCODE_ENABLE=true"), properties(BarcodePlugin(BarcodeTypes.QRCODE, null, null, log)))
  }

  @Test
  fun zeroLengthsDisableTheLengthCheck() {
    assertEquals(
      listOf("CODE39_ENABLE=true", "CODE39_LENGTH_CONTROL=0"),
      properties(BarcodePlugin(BarcodeTypes.CODE39, 0, 0, log)),
    )
  }

  @Test
  fun gs1AndCode128TogetherEmitOnlyCode128Lengths() {
    // A profile enabling both, each with the same length: the length control belongs to Code 128.
    val props = properties(
      BarcodePlugin(BarcodeTypes.EAN128, 20, 20, log),
      BarcodePlugin(BarcodeTypes.CODE128, 20, 20, log),
    )
    assertEquals(
      listOf("CODE128_GS1_ENABLE=true", "CODE128_ENABLE=true", "CODE128_LENGTH_CONTROL=3", "CODE128_LENGTH1=20", "CODE128_LENGTH2=20"),
      props,
    )
  }
}
