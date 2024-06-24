import android.nfc.tech.IsoDep

public actual sealed class NfcCard {

    public actual class Ndef(ndef: android.nfc.tech.Ndef) : NfcCard() {
        public actual val id: ByteArray = ndef.tag.id
        public actual val message: NdefMessage =
            NdefMessage.from(ndef.cachedNdefMessage.toByteArray())
    }

    public actual class NfcA(nfcA: android.nfc.tech.NfcA) : NfcCard() {
        public actual val id: ByteArray = nfcA.tag.id
        public actual val atqa: ByteArray = nfcA.atqa
        public actual val sak: Short = nfcA.sak
    }

    public actual class NfcB(nfcB: android.nfc.tech.NfcB) : NfcCard() {
        public actual val id: ByteArray = nfcB.tag.id
        public actual val appData: ByteArray = nfcB.applicationData
        public actual val protocolInfo: ByteArray = nfcB.protocolInfo
    }

    public actual class NfcF(nfcF: android.nfc.tech.NfcF) : NfcCard() {
        public actual val id: ByteArray = nfcF.tag.id
        public actual val systemCode: ByteArray = nfcF.systemCode
        public actual val manufacturer: ByteArray = nfcF.manufacturer
    }

    public actual class NfcV(nfcV: android.nfc.tech.NfcV) : NfcCard() {
        public actual val id: ByteArray = nfcV.tag.id
        public actual val responseFlags: Byte = nfcV.responseFlags
        public actual val dsfId: Byte = nfcV.dsfId
    }

    public actual sealed class IsoDep(public actual val id: ByteArray) : NfcCard() {

        public actual class A(isoDep: android.nfc.tech.IsoDep) : IsoDep(isoDep.tag.id) {
            private val nfcA = android.nfc.tech.NfcA.get(isoDep.tag)
            public actual val historicalBytes: ByteArray = isoDep.historicalBytes!!
            public actual val atqa: ByteArray = nfcA.atqa
            public actual val sak: Short = nfcA.sak
        }

        public actual class B(isoDep: android.nfc.tech.IsoDep) : IsoDep(isoDep.tag.id) {
            private val nfcB = android.nfc.tech.NfcB.get(isoDep.tag)
            public actual val hiLayerResponse: ByteArray = isoDep.hiLayerResponse
            public actual val appData: ByteArray = nfcB.applicationData
            public actual val protocolInfo: ByteArray = nfcB.protocolInfo
        }

        public companion object {
            public fun from(isoDep: android.nfc.tech.IsoDep): IsoDep {
                return if (isoDep.tag.techList.contains(android.nfc.tech.NfcA::class.qualifiedName!!)) {
                    A(isoDep)
                } else if (isoDep.tag.techList.contains(android.nfc.tech.NfcB::class.qualifiedName!!)) {
                    B(isoDep)
                } else {
                    throw IllegalArgumentException("unknown IsoDep variant")
                }
            }
        }
    }
}