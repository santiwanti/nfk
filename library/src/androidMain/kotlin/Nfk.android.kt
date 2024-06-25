import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcBarcode
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import utils.openNfcSettings
import java.lang.ref.WeakReference
import kotlin.coroutines.resume

public actual class Nfk(activity: Activity) {
    private val context: WeakReference<Activity> = WeakReference(activity)
    private val requiredContext: Activity
        get() = context.get()
            ?: throw IllegalStateException("the activity used to initialize Nfk has been garbage collected")
    private val nfcAdapter: NfcAdapter = NfcAdapter.getDefaultAdapter(activity)

    public actual fun isEnabled(): Boolean = nfcAdapter.isEnabled

    public actual suspend fun enable() {
        requiredContext.openNfcSettings()
    }

    public actual suspend fun read(timeout: Long?): NfcCard? = withContext(Dispatchers.IO) {
        withTimeout(timeout ?: Long.MAX_VALUE) {
            suspendCancellableCoroutine { cont ->
                nfcAdapter.enableReaderMode(
                    requiredContext,
                    { tag ->
                        println("read ${tag.id} with techList: ${tag.techList.joinToString()}")
                        val nfcTag = NfcTag.from(tag.techList)
                            ?: throw IllegalArgumentException("unsupported card")
                        val card = when (nfcTag) {
                            NfcTag.Ndef -> NfcCard.Ndef(Ndef.get(tag))
                            NfcTag.NfcA -> NfcCard.NfcA(NfcA.get(tag))
                            NfcTag.NfcB -> NfcCard.NfcB(NfcB.get(tag))
                            NfcTag.NfcF -> NfcCard.NfcF(NfcF.get(tag))
                            NfcTag.NfcV -> NfcCard.NfcV(NfcV.get(tag))
                            NfcTag.IsoDep -> NfcCard.IsoDep.from(IsoDep.get(tag))
                            NfcTag.MifareClassic -> NfcCard.MifareClassic(MifareClassic.get(tag))
                            NfcTag.MifareUltralight -> NfcCard.MifareUltralight(
                                MifareUltralight.get(
                                    tag
                                )
                            )

                            NfcTag.NfcBarcode -> NfcCard.NfcBarcode.from(NfcBarcode.get(tag))
                            NfcTag.NdefFormatable -> NfcCard.NdefFormatable(NdefFormatable.get(tag))
                        }
                        nfcAdapter.disableReaderMode(requiredContext)
                        cont.resume(card)
                    },
                    NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_NFC_BARCODE or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                    Bundle().apply {
                        putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
                    }
                )
                cont.invokeOnCancellation {
                    println("disabling reader mode")
                    nfcAdapter.disableReaderMode(requiredContext)
                    cont.resume(null)
                }
                println("setup the suspendCancellableCoroutine")
            }
        }
    }


    /**
     * Configure NFC to deliver new tags using the given pending intent. Also gives us priority
     * over all other system apps. Call in onResume()
     */
    private fun enableForegroundDispatch() {
        val intent = Intent(requiredContext, requiredContext.javaClass)
        val pendingIntent =
            PendingIntent.getActivity(requiredContext, 0, intent, PendingIntent.FLAG_MUTABLE)

        // Register the activity, pass null techLists as a wildcard
        nfcAdapter.enableForegroundDispatch(requiredContext, pendingIntent, null, null)
    }

    public actual companion object {
        private var instance: Nfk? = null

        public actual fun getInstance(): Nfk {
            return instance ?: throw IllegalStateException("NFK has not been properly initialized")
        }

        public fun init(activity: Activity): Nfk {
            return Nfk(activity).also {
                instance = it
            }
        }
    }

    // Note the order of this enum and the list are important. They must have the same order and
    //  IsoDep and Mifares have to be at the top because they have other techs like nfcA, so if
    //  nfcA is first it will be considered nfcA instead of the correct one. NdefFormatable must
    //  always be last
    private enum class NfcTag {
        IsoDep,
        MifareClassic,
        MifareUltralight,
        NfcA,
        NfcB,
        NfcF,
        NfcV,
        Ndef,
        NfcBarcode,
        NdefFormatable;

        companion object {
            private val techList = listOf(
                android.nfc.tech.IsoDep::class.qualifiedName!!,
                android.nfc.tech.MifareClassic::class.qualifiedName!!,
                android.nfc.tech.MifareUltralight::class.qualifiedName!!,
                android.nfc.tech.NfcA::class.qualifiedName!!,
                android.nfc.tech.NfcB::class.qualifiedName!!,
                android.nfc.tech.NfcF::class.qualifiedName!!,
                android.nfc.tech.NfcV::class.qualifiedName!!,
                android.nfc.tech.Ndef::class.qualifiedName!!,
                android.nfc.tech.NfcBarcode::class.qualifiedName!!,
                android.nfc.tech.NdefFormatable::class.qualifiedName!!,
            )

            fun from(techs: Array<String>): NfcTag? {
                for (tech in techs) {
                    techList.indexOf(tech).takeIf { it >= 0 }?.let { index ->
                        return NfcTag.entries[index]
                    }
                }
                return null
            }
        }
    }
}