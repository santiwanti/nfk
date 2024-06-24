public expect class Nfk {

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
     * @param timeout milliseconds to wait before cancelling. If `null` it will wait indefinitely.
     * @return the `NfcCard` that was read or `null`.
     */
    public suspend fun read(timeout: Long? = null): NfcCard?

    public companion object {
        public fun getInstance(): Nfk
    }
}