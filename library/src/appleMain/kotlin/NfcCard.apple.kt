public actual sealed class NfcCard {
    public actual class Ndef(public actual val message: NdefMessage) : NfcCard()
}