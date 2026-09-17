package no.talgoe.scanwedge.scanwedge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class NewlandScannerSettingsTest {
  @Test
  fun apiOutputIsSentEvenWithoutAConfig() {
    assertEquals(listOf<Pair<String, Any>>("EXTRA_SCAN_MODE" to 3), newlandScannerSettings(null))
  }

  @Test
  fun flagsBecomeOneAndZero() {
    val settings = newlandScannerSettings(hashMapOf("soundOnScan" to false, "ledOnScan" to true))

    assertTrue(settings.contains("EXTRA_SCAN_NOTY_SND" to 0))
    assertTrue(settings.contains("EXTRA_SCAN_NOTY_LED" to 1))
  }

  @Test
  fun durationsGoOutAsLongs() {
    val settings = newlandScannerSettings(hashMapOf("scanTimeout" to 7000))

    assertTrue(settings.contains("SCAN_TIMEOUT" to 7000L), "the device wants a long, got $settings")
  }

  @Test
  fun anUnknownTriggerModeIsLeftOut() {
    val settings = newlandScannerSettings(hashMapOf("triggerMode" to "nonsense"))

    assertTrue(settings.none { it.first == "EXTRA_TRIG_MODE" })
  }
}
