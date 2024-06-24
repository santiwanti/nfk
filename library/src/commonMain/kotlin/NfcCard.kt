public expect sealed class NfcCard {
    public class Ndef : NfcCard {
        public val id: ByteArray
        public val message: NdefMessage
    }

    public class NfcA : NfcCard {
        public val id: ByteArray
        public val atqa: ByteArray
        public val sak: Short
    }

    public class NfcB : NfcCard {
        public val id: ByteArray
        public val appData: ByteArray
        public val protocolInfo: ByteArray
    }

    public class NfcF : NfcCard {
        public val id: ByteArray
        public val systemCode: ByteArray
        public val manufacturer: ByteArray
    }

    public class NfcV : NfcCard {
        public val id: ByteArray
        public val responseFlags: Byte
        public val dsfId: Byte
    }

    public sealed class IsoDep : NfcCard {
        public val id: ByteArray

        public class A : IsoDep {
            public val historicalBytes: ByteArray
            public val atqa: ByteArray
            public val sak: Short
        }

        public class B : IsoDep {
            public val hiLayerResponse: ByteArray
            public val appData: ByteArray
            public val protocolInfo: ByteArray
        }
    }
}
