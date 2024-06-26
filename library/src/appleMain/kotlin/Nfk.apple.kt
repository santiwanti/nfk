import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreNFC.NFCNDEFMessage
import platform.CoreNFC.NFCNDEFPayload
import platform.CoreNFC.NFCNDEFReaderSession
import platform.CoreNFC.NFCNDEFReaderSessionDelegateProtocol
import platform.CoreNFC.NFCPollingISO14443
import platform.CoreNFC.NFCPollingISO15693
import platform.CoreNFC.NFCPollingISO18092
import platform.CoreNFC.NFCReaderSessionInvalidationErrorFirstNDEFTagRead
import platform.CoreNFC.NFCTagProtocol
import platform.CoreNFC.NFCTagReaderSession
import platform.CoreNFC.NFCTagReaderSessionDelegateProtocol
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
            val tagSession = NFCTagReaderSession(
                NFCPollingISO14443 and NFCPollingISO15693 and NFCPollingISO18092,
                object : NSObject(), NFCTagReaderSessionDelegateProtocol {
                    override fun tagReaderSession(
                        session: NFCTagReaderSession,
                        didInvalidateWithError: NSError
                    ) {
                        println("invalidated with error code: ${didInvalidateWithError.code}")
                    }

                    override fun tagReaderSession(
                        session: NFCTagReaderSession,
                        didDetectTags: List<*>
                    ) {
                        val tag = didDetectTags.firstOrNull() as? NFCTagProtocol
                            ?: throw IllegalArgumentException("couldn't find tag")
                        println("tag type: ${tag.type}")
                    }

                    override fun tagReaderSessionDidBecomeActive(session: NFCTagReaderSession) {
                        println("tag session is active")
                    }
                },
                null
            )
            tagSession.beginSession()

//            val ndefSession =
//                NFCNDEFReaderSession(object : NSObject(), NFCNDEFReaderSessionDelegateProtocol {
//                    override fun readerSession(
//                        session: NFCNDEFReaderSession,
//                        didDetectNDEFs: List<*>
//                    ) {
//                        val message = didDetectNDEFs.firstOrNull() as? NFCNDEFMessage
//                        val records = (message?.records as List<NFCNDEFPayload>).map {
//                            NdefRecord.from(
//                                it.typeNameFormat.toShort() and 0x0F,
//                                it.type.toByteArray(),
//                                it.identifier.toByteArray(),
//                                it.payload.toByteArray(),
//                            )
//                        }
//
//                        cont.resume(NfcCard.Ndef(NdefMessage(records)))
//                    }
//
//                    override fun readerSessionDidBecomeActive(session: NFCNDEFReaderSession) {
//                    }
//
//                    override fun readerSession(
//                        session: NFCNDEFReaderSession,
//                        didInvalidateWithError: NSError
//                    ) {
//                        // Only throw error if it's not because we invalidating after reading an element
//                        if (didInvalidateWithError.code != NFCReaderSessionInvalidationErrorFirstNDEFTagRead) cont.resumeWithException(
//                            IllegalStateException(didInvalidateWithError.description)
//                        )
//                    }
//                }, null, true)
//            ndefSession.beginSession()
        }
    }

    public actual companion object {
        private val instance = Nfk()
        public actual fun getInstance(): Nfk {
            return instance
        }
    }
}
