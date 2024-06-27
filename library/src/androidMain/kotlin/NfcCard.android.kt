import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import utils.EnumWithValue
import utils.emptyByteArray
import utils.getByValue
import kotlin.experimental.and

public actual sealed class NfcCard {

    public actual class Ndef(private val ndef: android.nfc.tech.Ndef) : NfcCard() {
        public actual val id: ByteArray = ndef.tag.id
        public actual val message: NdefMessage =
            NdefMessage.from(ndef.cachedNdefMessage.toByteArray())
    }

    public actual class NfcA(private val nfcA: android.nfc.tech.NfcA) : NfcCard() {
        public actual val id: ByteArray = nfcA.tag.id
        public actual val atqa: ByteArray = nfcA.atqa
        public actual val sak: Short = nfcA.sak
    }

    public actual class NfcB(private val nfcB: android.nfc.tech.NfcB) : NfcCard() {
        public actual val id: ByteArray = nfcB.tag.id
        public actual val appData: ByteArray = nfcB.applicationData
        public actual val protocolInfo: ByteArray = nfcB.protocolInfo
    }

    public actual class NfcF(private val nfcF: android.nfc.tech.NfcF) : NfcCard() {
        public actual val id: ByteArray = nfcF.tag.id
        public actual val systemCode: ByteArray = nfcF.systemCode
        public actual val manufacturer: ByteArray = nfcF.manufacturer
    }

    public actual class NfcV(private val nfcV: android.nfc.tech.NfcV) : NfcCard() {
        public actual val id: ByteArray = nfcV.tag.id
        public actual val responseFlags: Byte = nfcV.responseFlags
        public actual val dsfId: Byte = nfcV.dsfId
    }

    public actual sealed class IsoDep(public actual val id: ByteArray) : NfcCard() {

        public actual class A(private val isoDep: android.nfc.tech.IsoDep) : IsoDep(isoDep.tag.id) {
            private val nfcA = android.nfc.tech.NfcA.get(isoDep.tag)
            public actual val historicalBytes: ByteArray = isoDep.historicalBytes!!
            public actual val atqa: ByteArray = nfcA.atqa
            public actual val sak: Short = nfcA.sak
        }

        public actual class B(private val isoDep: android.nfc.tech.IsoDep) : IsoDep(isoDep.tag.id) {
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

    public actual class MifareClassic(private val mifareClassic: android.nfc.tech.MifareClassic) :
        NfcCard() {
        public actual val id: ByteArray = mifareClassic.tag.id
        public actual val type: Type = Type.entries[mifareClassic.type + 1]
        public actual val size: Size = Size.getByValue(mifareClassic.size)
        public actual val sectorCount: Int = mifareClassic.sectorCount
        public actual val blockCount: Int = mifareClassic.blockCount
        public actual var keyA: ByteArray? = null
            set(value) {
                require(value?.size == 6)
                field = value
            }
        public actual var keyB: ByteArray? = null
            set(value) {
                require(value?.size == 6)
                field = value
            }
        public actual var sectorsForKeyA: List<Int> = emptyList()
        public actual var sectorsForKeyB: List<Int> = emptyList()

        public actual fun blocksInSector(sectorIndex: Int): Int {
            return mifareClassic.getBlockCountInSector(sectorIndex)
        }

        public actual fun getSectorForBlock(blockIndex: Int): Int {
            return mifareClassic.blockToSector(blockIndex)
        }

        public actual fun getFirstBlockOfSector(sectorIndex: Int): Int {
            return mifareClassic.sectorToBlock(sectorIndex)
        }

        public actual suspend fun readBlock(blockIndex: Int): ByteArray =
            withContext(Dispatchers.IO) {
                if (!mifareClassic.isConnected) mifareClassic.connect()

                val sector = getSectorForBlock(blockIndex)
                if (sectorsForKeyA.contains(sector) && keyA != null) {
                    mifareClassic.authenticateSectorWithKeyA(sector, keyA)
                } else if (sectorsForKeyB.contains(sector) && keyB != null) {
                    mifareClassic.authenticateSectorWithKeyB(sector, keyB)
                } else {
                    throw IllegalStateException("don't have key to authenticate to this block's sector ($sector)")
                }

                mifareClassic.readBlock(blockIndex)
            }

        public actual enum class Type {
            Unknown, Classic, Plus, Pro
        }

        public actual enum class Size(public actual val size: Int) {
            MINI(320), _1K(1024), _2K(2048), _4K(4096);

            internal companion object : EnumWithValue<Size, Int> {
                override val Size.value: Int
                    get() = size
            }
        }
    }

    public actual class MifareUltralight(private val mifareUltralight: android.nfc.tech.MifareUltralight) :
        NfcCard() {
        public actual val id: ByteArray = mifareUltralight.tag.id
        public actual val type: Type =
            if (mifareUltralight.type < 0) Type.Unknown else Type.entries[mifareUltralight.type]

        public actual suspend fun readFourPages(pageOffset: Int): ByteArray =
            withContext(Dispatchers.IO) {
                if (!mifareUltralight.isConnected) mifareUltralight.connect()
                mifareUltralight.readPages(pageOffset)
            }

        // TODO improve by not creating 16 ByteArrays
        public actual suspend fun readAllPages(): ByteArray = withContext(Dispatchers.IO) {
            var result = emptyByteArray()

            for (i in 0 until 4) {
                result = result.plus(readFourPages(pageOffset = i * 4))
            }

            if (type == Type.UltraLightC) {
                for (i in 4 until 11) {
                    result = result.plus(readFourPages(pageOffset = i * 4))
                }
            }

            result
        }

        public actual enum class Type {
            Unknown, UltraLight, UltraLightC;
        }
    }

    public actual sealed class NfcBarcode(
        private val nfcBarcode: android.nfc.tech.NfcBarcode,
        public actual val id: ByteArray
    ) : NfcCard() {

        public actual sealed class Kovio(
            nfcBarcode: android.nfc.tech.NfcBarcode,
            id: ByteArray,
            public actual val manufacturerId: Byte
        ) :
            NfcBarcode(nfcBarcode, id) {
            public actual class Url(
                nfcBarcode: android.nfc.tech.NfcBarcode,
                id: ByteArray,
                manufacturerId: Byte,
                public actual val url: String,
            ) : Kovio(nfcBarcode, id, manufacturerId)

            public actual class Epc(
                nfcBarcode: android.nfc.tech.NfcBarcode,
                id: ByteArray,
                manufacturerId: Byte,
                public actual val payload: ByteArray,
            ) : Kovio(nfcBarcode, id, manufacturerId)

            public actual class Unknown(
                nfcBarcode: android.nfc.tech.NfcBarcode,
                id: ByteArray,
                manufacturerId: Byte,
                public actual val barcode: ByteArray,
            ) : Kovio(nfcBarcode, id, manufacturerId)

            public actual class NotSetup(
                nfcBarcode: android.nfc.tech.NfcBarcode,
                id: ByteArray,
                manufacturerId: Byte,
                public actual val barcode: ByteArray,
            ) : Kovio(nfcBarcode, id, manufacturerId)

            internal companion object {
                // TODO verify the CRC and all that
                internal fun from(id: ByteArray, nfcBarcode: android.nfc.tech.NfcBarcode): Kovio {
                    val manufacturerId = nfcBarcode.barcode.first() and 0x7F
                    val secondByte = nfcBarcode.barcode[1].toInt()
                    val payload = nfcBarcode.barcode.sliceArray(IntRange(2, 13))
                    return when (secondByte) {
                        0 -> NotSetup(nfcBarcode, id, manufacturerId, payload)
                        in 1..4 -> {
                            val prefix = when (secondByte) {
                                1 -> "http://www."
                                2 -> "https://www."
                                3 -> "http://"
                                4 -> "https://"
                                else -> throw IllegalStateException("unreachable")
                            }
                            Url(
                                nfcBarcode,
                                id,
                                manufacturerId,
                                "$prefix${payload.decodeToString()}"
                            )
                        }

                        5 -> Epc(nfcBarcode, id, manufacturerId, payload)
                        else -> Unknown(nfcBarcode, id, manufacturerId, payload)
                    }
                }
            }
        }

        public actual class Unknown(
            nfcBarcode: android.nfc.tech.NfcBarcode,
            id: ByteArray,
            public actual val barcode: ByteArray
        ) :
            NfcBarcode(nfcBarcode, id)

        public companion object {
            public fun from(nfcBarcode: android.nfc.tech.NfcBarcode): NfcBarcode {
                val id: ByteArray = nfcBarcode.tag.id
                val type = nfcBarcode.type
                return if (type == android.nfc.tech.NfcBarcode.TYPE_UNKNOWN) {
                    Unknown(nfcBarcode, id, nfcBarcode.barcode)
                } else {
                    Kovio.from(id, nfcBarcode)
                }
            }
        }
    }

    public actual class NdefFormatable(private val ndefFormatable: android.nfc.tech.NdefFormatable) :
        NfcCard() {
        public actual val id: ByteArray = ndefFormatable.tag.id
    }
}