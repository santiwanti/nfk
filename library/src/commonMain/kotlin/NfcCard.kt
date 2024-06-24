public expect sealed class NfcCard {
    public class Ndef: NfcCard {
        public val message: NdefMessage
    }
}
