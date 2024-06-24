import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCNDEFReaderSessionDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject
import utils.toByteArray
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.and

public actual class Nfk {

    /**
     *
     * @return true if enabled, false otherwise
     */
    public actual fun isEnabled(): Boolean {
        return NFCNDEFReaderSession.readingAvailable()
    }

    /**
     * Does whatever can be done to enable NFC. After this function runs there is no guarantee that
     * NFC is enabled.
     */
    public actual suspend fun enable() {
    }

    /**
     * Attempts to read a single tag.
     *
     * @param timeout milliseconds to wait before cancelling. If `null` it will wait indefinitely.
     * @return the `NfcCard` that was read or `null`.
     */
    public actual suspend fun read(timeout: Long?): NfcCard? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { cont ->
            NFCNDEFReaderSession(object : NSObject(), NFCNDEFReaderSessionDelegateProtocol {
                override fun readerSession(session: NFCNDEFReaderSession, didDetectNDEFs: List<*>) {
                    val message = didDetectNDEFs.firstOrNull() as? NFCNDEFMessage
                    val records = (message?.records as List<NFCNDEFPayload>).map {
                        NdefRecord.from(
                            it.typeNameFormat.toShort() and 0x0F,
                            it.type.toByteArray(),
                            it.identifier.toByteArray(),
                            it.payload.toByteArray(),
                        )
                    }

                    cont.resume(NfcCard.Ndef(NdefMessage(records)))
                }

                override fun readerSessionDidBecomeActive(session: NFCNDEFReaderSession) {
                }

                override fun readerSession(
                    session: NFCNDEFReaderSession,
                    didInvalidateWithError: NSError
                ) {
                    cont.resumeWithException(IllegalStateException(didInvalidateWithError.description))
                }
            }, null, true)
        }
    }

    public actual companion object {
        private val instance = Nfk()
        public actual fun getInstance(): Nfk {
            return instance
        }
    }
}
