import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.tech.Ndef
import android.os.Bundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import utils.openNfcSettings
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public actual class Nfk(activity: Activity) {
    private val context: WeakReference<Activity> = WeakReference(activity)
    private val requiredContext: Activity
        get() = context.get() ?: throw IllegalStateException("the activity used to initialize Nfk has been garbage collected")
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
                        nfcAdapter.disableReaderMode(requiredContext)
                        println("read ${tag.id}")
                        Ndef.get(tag)?.let {
                            cont.resume(NfcCard.Ndef(it))
                        }
                            ?: cont.resumeWithException(IllegalArgumentException("only NDEF cards are handled for now"))
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
}