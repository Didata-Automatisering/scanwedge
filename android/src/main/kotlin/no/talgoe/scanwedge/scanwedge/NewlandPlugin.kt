package no.talgoe.scanwedge.scanwedge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

// Hardware plugin for Newland devices that extends the IHardwarePlugin interface.
class NewlandPlugin(private val scanW: ScanwedgePlugin, private val log: Logger?) : IHardwarePlugin {
    companion object {
        private const val NL_SCAN_ACTION = "nlscan.action.SCANNER_RESULT"
        private const val ACTION_BAR_SCANCFG = "ACTION_BAR_SCANCFG"
        private const val ACTION_BARCODE_CFG = "ACTION_BARCODE_CFG"
        private const val EXTRA_SCAN_MODE = "EXTRA_SCAN_MODE"
        private const val SCAN_MODE_OUTPUT_VIA_API = 3
        private const val SEND_SCAN_FAIL_BROADCAST = "SEND_SCAN_FAIL_BROADCAST"
        private const val TAG="NewlandPlugin"
    }

    private val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                log?.i(TAG, "onReceive: ${intent.toUri(0)}, ${intent.action}")
                if (intent.action != NL_SCAN_ACTION)
                    return

                val barcodeData = intent.getStringExtra("SCAN_BARCODE1")
                val codeId = intent.getStringExtra("SCAN_BARCODE_TYPE_NAME")
                if (barcodeData.isNullOrEmpty() || codeId == null) {
                    // A failed trigger pull lands here too, which is routine rather than an error
                    if (intent.getStringExtra("SCAN_STATE") == "fail") log?.i(TAG, "Scan failed")
                    else log?.e(TAG, "barcode or codeId is null")
                    return
                }
                val barcodeType = BarcodeTypes.fromNewlandCode(codeId)

                log?.i(TAG, "Barcode Data: $barcodeData, Barcode Type: $barcodeType")
                scanW.sendScanResult(ScanResult(barcodeData, barcodeType, codeId))
            } catch (e: Exception) {
                log?.e(TAG, "Error in barcodeDataReceiver: ${e.message}")
            }
        }
    }

    override val apiVersion: String get() = "NEWLAND"


    override fun initialize(context: Context?): Boolean {
        log?.i(TAG, "$TAG initializing")
        if (context == null)
            return false

        val filter = IntentFilter(ScanwedgePlugin.SCANWEDGE_ACTION)
        filter.addAction(NL_SCAN_ACTION)
        return try {
            context.registerReceiverCompat(barcodeDataReceiver, filter, exported = false)
            true
        } catch (e: Exception) {
            log?.e(TAG, "$TAG initialize, Exception: ${e.message}")
            false
        }
    }

    override fun createProfile(
        name: String,
        enabledBarcodes: List<BarcodePlugin>?,
        hwConfig: HashMap<String, Any>?,
        keepDefaults: Boolean
    ): Boolean {
        log?.i(TAG, "createProfile($name, $enabledBarcodes, $hwConfig, $keepDefaults)")

        // Any other output mode types into the focused field, and the receiver above never fires.
        sendScannerSetting(EXTRA_SCAN_MODE, SCAN_MODE_OUTPUT_VIA_API)
        @Suppress("UNCHECKED_CAST")
        val newlandConfig = hwConfig?.get("newland") as? HashMap<String, Any>
        (newlandConfig?.get("sendScanFailBroadcast") as? Boolean)?.let {
            sendScannerSetting(SEND_SCAN_FAIL_BROADCAST, if(it) 1 else 0)
        }

        val settings = newlandBarcodeSettings(enabledBarcodes, keepDefaults)
        log?.i(TAG, "createProfile: $settings")
        for(setting in settings){
            scanW.sendBroadcast(Intent(ACTION_BARCODE_CFG).apply{
                putExtra("CODE_ID", setting.codeId)
                putExtra("PROPERTY", setting.property)
                putExtra("VALUE", setting.value)
            })
        }
        return true
    }

    // One extra per broadcast: the handbook caps ACTION_BAR_SCANCFG at three.
    private fun sendScannerSetting(key: String, value: Int) {
        scanW.sendBroadcast(Intent(ACTION_BAR_SCANCFG).putExtra(key, value))
    }

    override fun enableScanner(): Boolean {
        log?.w(TAG, "Cannot programmatically control scanner")
        return false
    }

    override fun disableScanner(): Boolean {
        log?.w(TAG, "Cannot programmatically control scanner")
        return false
    }

    override fun toggleScanning(): Boolean {
        log?.w(TAG, "Cannot programmatically control scanner")
        return false
    }

    override fun dispose(context: Context?) {
        context?.unregisterReceiverSafely(barcodeDataReceiver)
    }
}

// Newland publishes no list of symbologies that are on out of the box, so `keepDefaults = false`
// switches off everything nameable that was not asked for rather than a known default set.
internal fun newlandBarcodeSettings(
    enabledBarcodes: List<BarcodePlugin>?,
    keepDefaults: Boolean,
): ArrayList<NewlandBarcodeSetting> {
    val settings = ArrayList<NewlandBarcodeSetting>()
    enabledBarcodes?.forEach { it.newlandAddToList(settings) }
    if (keepDefaults) return settings

    val enabledTypes = enabledBarcodes?.map { it.type } ?: emptyList()
    BarcodeTypes.values()
        .filter { it !in enabledTypes && it.newlandDecoderName() != null }
        .forEach { it.newlandDisableBarcode(settings) }

    return settings
}
