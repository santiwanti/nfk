public actual sealed class NfcCard {


    public actual class Ndef(ndef: android.nfc.tech.Ndef) : NfcCard() {
        public actual val message: NdefMessage =
            NdefMessage.from(ndef.cachedNdefMessage.toByteArray())
    }
}