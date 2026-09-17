package no.talgoe.scanwedge.scanwedge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/*
* Newland takes one ACTION_BARCODE_CFG broadcast per symbology per property, so a profile is a list
* of triples rather than one bundle. These pin what goes out.
*
* Run with `./gradlew testDebugUnitTest` in `example/android/`.
*/
internal class NewlandProfileTest {
  private val log = TestLogger()
  private fun plugin(type: BarcodeTypes) = BarcodePlugin(type, null, null, log)

  @Test
  fun keepingDefaultsOnlyEnablesWhatWasAskedFor() {
    val settings = newlandBarcodeSettings(listOf(plugin(BarcodeTypes.CODE128)), keepDefaults = true)

    assertEquals(listOf(NewlandBarcodeSetting("CODE128", "Enable", "1")), settings)
  }

  @Test
  fun droppingDefaultsDisablesEverythingElseItCanName() {
    val settings = newlandBarcodeSettings(listOf(plugin(BarcodeTypes.CODE128)), keepDefaults = false)

    assertEquals(NewlandBarcodeSetting("CODE128", "Enable", "1"), settings.first())
    assertTrue(settings.any { it == NewlandBarcodeSetting("CODE39", "Enable", "0") })
    assertTrue(settings.none { it.codeId == "CODE128" && it.value == "0" }, "must not undo its own enable")
  }

  @Test
  fun twoTypesSharingACodeIdDoNotCancelEachOther() {
    val shared = BarcodeTypes.GS1_DATABAR.newlandDecoderName()!!
    val settings = newlandBarcodeSettings(listOf(plugin(BarcodeTypes.GS1_DATABAR)), keepDefaults = false)

    assertEquals(
      listOf(NewlandBarcodeSetting(shared, "Enable", "1")),
      settings.filter { it.codeId == shared },
    )
  }

  @Test
  fun aTypeNewlandCannotNameIsSkipped() {
    val settings = newlandBarcodeSettings(listOf(plugin(BarcodeTypes.MAILMARK)), keepDefaults = true)

    assertEquals(emptyList(), settings)
  }
}
