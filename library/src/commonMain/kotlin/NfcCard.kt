import utils.EnumWithValue

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

    public class MifareClassic : NfcCard {
        public val id: ByteArray
        public val type: Type
        public val size: Size
        public val sectorCount: Int
        public val blockCount: Int
        public var keyA: ByteArray?
        public var keyB: ByteArray?
        public var sectorsForKeyA: List<Int>
        public var sectorsForKeyB: List<Int>

        /**
         * MIFARE Classic Mini are 320 bytes (SIZE_MINI), with 5 sectors each of 4 blocks.
         * MIFARE Classic 1k are 1024 bytes (SIZE_1K), with 16 sectors each of 4 blocks.
         * MIFARE Classic 2k are 2048 bytes (SIZE_2K), with 32 sectors each of 4 blocks.
         * MIFARE Classic 4k are 4096 bytes (SIZE_4K). The first 32 sectors contain 4 blocks and the last 8 sectors contain 16 blocks.
         */
        public fun blocksInSector(sectorIndex: Int): Int
        public fun getSectorForBlock(blockIndex: Int): Int
        public fun getFirstBlockOfSector(sectorIndex: Int): Int

        public suspend fun readBlock(blockIndex: Int): ByteArray
//        public suspend fun writeBlock(blockIndex: Int, data: ByteArray)

//        public suspend fun increment(blockIndex: Int, value: Int)
//        public suspend fun decrement(blockIndex: Int, value: Int)

        public enum class Type {
            Unknown, Classic, Plus, Pro;
        }

        public enum class Size {
            MINI, _1K, _2K, _4K;

            public val size: Int

        }
    }

    /**
     * A page is 4 bytes.
     */
    public class MifareUltralight : NfcCard {
        public val id: ByteArray
        public val type: Type

        public suspend fun readFourPages(pageOffset: Int): ByteArray
        public suspend fun readAllPages(): ByteArray

        public enum class Type {
            Unknown, UltraLight, UltraLightC;
        }
    }

    public sealed class NfcBarcode : NfcCard {
        public val id: ByteArray

        public sealed class Kovio : NfcBarcode {
            public val manufacturerId: Byte

            public class Url : Kovio {
                public val url: String
            }

            // TODO figure out if this can be broken down: https://www.gs1.org/standards/tds
            public class Epc : Kovio {
                public val payload: ByteArray
            }

            public class Unknown : Kovio {
                public val barcode: ByteArray
            }

            public class NotSetup: Kovio {
                public val barcode: ByteArray
            }
        }

        public class Unknown : NfcBarcode {
            public val barcode: ByteArray
        }
    }

    /**
     * This is writeOnly. you can't read anything from these tags
     */
    public class NdefFormatable : NfcCard {
        public val id: ByteArray
    }
}
