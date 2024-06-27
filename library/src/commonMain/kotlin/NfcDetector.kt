import kotlin.reflect.KClass

public interface NfcDetector {
    /**
     *
     * @return true if enabled, false otherwise
     */
    public fun isEnabled(): Boolean

    // TODO add a compose library that handles the enable more nicely
    /**
     * Does whatever can be done to enable NFC. After this function runs there is no guarantee that
     * NFC is enabled.
     */
    public suspend fun enable()

    /**
     * Attempts to read a single tag.
     *
     * @param cardTypesToDetect which card types we are looking for. If the detected tag is not of one
     * of the listed types the function will throw an `IllegalStateException`. if this parameter is
     * null it is equivalent to a list with all possible `NfcCard` types.
     * @param timeout milliseconds to wait before cancelling. If `null` it will wait indefinitely.
     * @return the `NfcCard` that was read or `null`.
     */
    public suspend fun <T : NfcCard> detect(
        cardTypesToDetect: List<KClass<T>>?,
        timeout: Long? = null,
    ): NfcCard?
}