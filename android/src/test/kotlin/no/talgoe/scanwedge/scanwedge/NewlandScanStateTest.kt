package no.talgoe.scanwedge.scanwedge

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NewlandScanStateTest {
  @Test
  fun failedScanIsDropped() {
    assertFalse(isSuccessfulNewlandScan("fail"))
  }

  @Test
  fun scanWithoutAStateStillCounts() {
    assertTrue(isSuccessfulNewlandScan(null), "firmware that never sends SCAN_STATE must keep working")
  }

  @Test
  fun okIsAScan() {
    assertTrue(isSuccessfulNewlandScan("ok"))
  }
}
