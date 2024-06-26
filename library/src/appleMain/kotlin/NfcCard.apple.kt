public actual sealed class NfcCard {
    public actual class Ndef(public actual val message: NdefMessage) : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
    }

    public actual class NfcA : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
        public actual val atqa: ByteArray
            get() = TODO("Not yet implemented")
        public actual val sak: Short
            get() = TODO("Not yet implemented")
    }

    public actual class NfcB : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
        public actual val appData: ByteArray
            get() = TODO("Not yet implemented")
        public actual val protocolInfo: ByteArray
            get() = TODO("Not yet implemented")
    }

    public actual class NfcF : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
        public actual val systemCode: ByteArray
            get() = TODO("Not yet implemented")
        public actual val manufacturer: ByteArray
            get() = TODO("Not yet implemented")
    }

    public actual class NfcV : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
        public actual val responseFlags: Byte
            get() = TODO("Not yet implemented")
        public actual val dsfId: Byte
            get() = TODO("Not yet implemented")
    }

    public actual sealed class IsoDep : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")

        public actual class A : IsoDep() {
            public actual val historicalBytes: ByteArray
                get() = TODO("Not yet implemented")
            public actual val atqa: ByteArray
                get() = TODO("Not yet implemented")
            public actual val sak: Short
                get() = TODO("Not yet implemented")
        }

        public actual class B : IsoDep() {
            public actual val hiLayerResponse: ByteArray
                get() = TODO("Not yet implemented")
            public actual val appData: ByteArray
                get() = TODO("Not yet implemented")
            public actual val protocolInfo: ByteArray
                get() = TODO("Not yet implemented")
        }

    }

    public actual class MifareClassic : NfcCard() {

        //        public suspend fun writeBlock(blockIndex: Int, data: ByteArray)

        //        public suspend fun increment(blockIndex: Int, value: Int)
//        public suspend fun decrement(blockIndex: Int, value: Int)
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
        public actual val type: Type
            get() = TODO("Not yet implemented")
        public actual val size: Size
            get() = TODO("Not yet implemented")
        public actual val sectorCount: Int
            get() = TODO("Not yet implemented")
        public actual val blockCount: Int
            get() = TODO("Not yet implemented")
        public actual var keyA: ByteArray?
            get() = TODO("Not yet implemented")
            set(value) {}
        public actual var keyB: ByteArray?
            get() = TODO("Not yet implemented")
            set(value) {}
        public actual var sectorsForKeyA: List<Int>
            get() = TODO("Not yet implemented")
            set(value) {}
        public actual var sectorsForKeyB: List<Int>
            get() = TODO("Not yet implemented")
            set(value) {}

        /**
         * MIFARE Classic Mini are 320 bytes (SIZE_MINI), with 5 sectors each of 4 blocks.
         * MIFARE Classic 1k are 1024 bytes (SIZE_1K), with 16 sectors each of 4 blocks.
         * MIFARE Classic 2k are 2048 bytes (SIZE_2K), with 32 sectors each of 4 blocks.
         * MIFARE Classic 4k are 4096 bytes (SIZE_4K). The first 32 sectors contain 4 blocks and the last 8 sectors contain 16 blocks.
         */
        public actual fun blocksInSector(sectorIndex: Int): Int {
            TODO("Not yet implemented")
        }

        public actual fun getSectorForBlock(blockIndex: Int): Int {
            TODO("Not yet implemented")
        }

        public actual fun getFirstBlockOfSector(sectorIndex: Int): Int {
            TODO("Not yet implemented")
        }

        public actual suspend fun readBlock(blockIndex: Int): ByteArray {
            TODO("Not yet implemented")
        }

        public actual enum class Type {
            Unknown, Classic, Plus, Pro;
        }

        public actual enum class Size {
            MINI, _1K, _2K, _4K;

            public actual val size: Int
                get() = TODO("Not yet implemented")

        }

    }

    /**
     * A page is 4 bytes.
     */
    public actual class MifareUltralight : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
        public actual val type: Type
            get() = TODO("Not yet implemented")

        public actual suspend fun readFourPages(pageOffset: Int): ByteArray {
            TODO("Not yet implemented")
        }

        public actual suspend fun readAllPages(): ByteArray {
            TODO("Not yet implemented")
        }

        public actual enum class Type {
            Unknown, UltraLight, UltraLightC;
        }

    }

    public actual sealed class NfcBarcode : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")

        public actual sealed class Kovio : NfcBarcode() {
            public actual val manufacturerId: Byte
                get() = TODO("Not yet implemented")

            public actual class Url : Kovio() {
                public actual val url: String
                    get() = TODO("Not yet implemented")
            }

            // TODO figure out if this can be broken down: https://www.gs1.org/standards/tds
            public actual class Epc : Kovio() {
                public actual val payload: ByteArray
                    get() = TODO("Not yet implemented")
            }

            public actual class Unknown : Kovio() {
                public actual val barcode: ByteArray
                    get() = TODO("Not yet implemented")
            }

            public actual class NotSetup : Kovio() {
                public actual val barcode: ByteArray
                    get() = TODO("Not yet implemented")
            }

        }

        public actual class Unknown : NfcBarcode() {
            public actual val barcode: ByteArray
                get() = TODO("Not yet implemented")
        }

    }

    /**
     * This is writeOnly. you can't read anything from these tags
     */
    public actual class NdefFormatable : NfcCard() {
        public actual val id: ByteArray
            get() = TODO("Not yet implemented")
    }
}