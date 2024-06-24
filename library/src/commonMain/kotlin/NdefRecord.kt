// TODO check if there is a way to omit this project wide. Or add tests idk. But would I check the ID in a test?
@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")

import utils.EnumWithValue
import utils.byteArrayOfNumber
import utils.emptyByteArray
import utils.encodeAsUtf16ByteArray
import utils.encodeAsUtf8ByteArray
import utils.getByValue
import utils.getNumber
import utils.plusNumber
import utils.toPositiveInt
import kotlin.experimental.and
import kotlin.experimental.or

// TODO add checks in `init` functions to make sure Records are setup properly

public sealed class NdefRecord(
    public val tnf: Short,
    public val type: ByteArray,
    public val id: ByteArray,
    public val payload: ByteArray,
) {

    public companion object {
        public fun parse(byteArray: ByteArray): List<NdefRecord> {
            // TODO look at androids NdefRecord.parse which returns a List<Record>
            val records = mutableListOf<NdefRecord>()
            var index = 0

            var type = emptyByteArray()
            var id = emptyByteArray()
            var payload: ByteArray

            val chunks = mutableListOf<ByteArray>()
            var inChunk = false
            var chunkTnf = Tnf.Empty.value

            do {
                val flag = byteArray[index++]

                val mb = (flag and FLAG_MB).toInt() != 0
                val me = (flag and FLAG_ME).toInt() != 0
                val cf = (flag and FLAG_CF).toInt() != 0
                val sr = (flag and FLAG_SR).toInt() != 0
                val il = (flag and FLAG_IL).toInt() != 0
                var tnf = (flag and 0x07).toShort()

                when {
                    !mb && records.isEmpty() && !inChunk -> throw IllegalArgumentException("expected MB (start) flag")
                    mb && (records.isNotEmpty() || inChunk) -> throw IllegalArgumentException("unexpected MB (start) flag")
                    inChunk && il -> throw IllegalArgumentException("unexpected IL flag in non-leading chunk")
                    cf && me -> throw IllegalArgumentException("unexpected ME flag in non-trailing chunk")
                    inChunk && tnf != Tnf.Unchanged.value -> throw IllegalArgumentException("expected TNF_UNCHANGED in non-leading chunk")
                    !inChunk && tnf == Tnf.Unchanged.value -> throw IllegalArgumentException("unexpected TNF_UNCHANGED in first chunk or unchunked record")
                }

                val typeLength = byteArray[index++].toPositiveInt()
                if (inChunk && typeLength != 0) throw IllegalArgumentException("expected zero-length type in non-leading chunk")
                val payloadLength: Int = if (sr) {
                    byteArray[index++].toPositiveInt()
                } else {
                    byteArray.getNumber(index, Int.SIZE_BYTES).also { index += Int.SIZE_BYTES }
                        .toInt()
                }
                if (payloadLength > MAX_PAYLOAD_SIZE) throw IllegalArgumentException("payload is larger than the max payload of: $MAX_PAYLOAD_SIZE")

                val idLength = if (il) byteArray[index++].toPositiveInt() else 0

                if (!inChunk) {
                    type = ByteArray(typeLength) { byteArray[index++] }
                    id = ByteArray(idLength) { byteArray[index++] }
                }

                // TODO we can use byteArray.sliceArray()
                payload = ByteArray(payloadLength) { byteArray[index++] }

                // Clear stuff given we have a new chunk
                if (cf && !inChunk) {
                    if (typeLength == 0 && tnf != Tnf.Unknown.value) throw IllegalArgumentException(
                        "expected non-zero type length in first chunk"
                    )
                    chunks.clear()
                    chunkTnf = tnf
                }

                // Do chunk stuff
                if (cf || inChunk) {
                    chunks.add(payload)
                }

                // Last chunk
                if (!cf && inChunk) {
                    val totalPayloadLength = chunks.fold(0) { acc, arr -> acc + arr.size }
                    if (totalPayloadLength > MAX_PAYLOAD_SIZE) throw IllegalArgumentException("payload is larger than the max payload of: $MAX_PAYLOAD_SIZE")
                    chunks.fold(emptyByteArray()) { acc, arr -> acc.plus(arr) }
                    tnf = chunkTnf
                }

                inChunk = cf
                if (inChunk) continue

                val record: NdefRecord = when (Tnf.entries.first { it.value == tnf }) {
                    Tnf.Empty -> Empty
                    Tnf.WellKnown -> {
                        when (Rtd.entries.first { it.value.contentEquals(type) }) {
                            Rtd.Text -> WellKnown.Text.from(id, payload)
                            Rtd.Uri -> WellKnown.Uri(payload.decodeToString())
                            Rtd.SmartPoster -> WellKnown.SmartPoster.from(id, payload)
                            Rtd.AltCarrier -> WellKnown.AlternativeCarrier.from(id, payload)
                            Rtd.HandoverCarrier -> WellKnown.HandoverCarrier.from(id, payload)
                            Rtd.HandoverRequest -> WellKnown.HandoverRequest.from(id, payload)
                            Rtd.HandoverSelect -> WellKnown.HandoverSelect.from(id, payload)
                            Rtd.CollisionResolution -> WellKnown.CollisionResolution(
                                id,
                                payload.getNumber(0, Short.SIZE_BYTES).toShort()
                            )

                            Rtd.Error -> WellKnown.Error.from(id, payload)
                        }
                    }

                    Tnf.Mime -> Mime(id, type.toString(), payload)
                    Tnf.Uri -> Uri(id, payload.decodeToString())
                    Tnf.Ext -> {
                        val (domain, extType) = type.decodeToString().split(":")
                        External(domain, extType, payload)
                    }

                    Tnf.Unknown -> Unknown(id, payload)
                    Tnf.Unchanged -> throw IllegalStateException("How did we get to an Unchanged full chunk?")
                    Tnf.Reserved -> throw IllegalStateException("this tnf is reserved, we shouldn't create a record with this tnf")
                }

                records.add(record)
            } while (!me)

            return records
        }

        // MessageBegin
        private const val FLAG_MB: Byte = 0x80.toByte()

        // MessageEnd
        private const val FLAG_ME: Byte = 0x40

        // Chunk Flag
        private const val FLAG_CF: Byte = 0x20

        // Short Record
        private const val FLAG_SR: Byte = 0x10

        // ID Length
        private const val FLAG_IL: Byte = 0x08

        private const val MAX_PAYLOAD_SIZE: Int = 10 * (1 shl 20) // 10 MB payload limit
    }

    // TODO could this be improved by not creating 20 different byteArrays?
    public fun toByteArray(mb: Boolean, me: Boolean): ByteArray {
        val sr: Boolean = payload.size < 256
        val il = if (tnf.toInt() == 0) true else id.isNotEmpty()

        val flags =
            ((if (mb) FLAG_MB else 0).toInt() or (if (me) FLAG_ME else 0).toInt() or
                    (if (sr) FLAG_SR else 0).toInt() or (if (il) FLAG_IL else 0).toInt() or tnf.toInt()).toByte()

        return byteArrayOf(flags).plus(type.size.toByte()).let {
            if (sr) it.plus(payload.size.toByte()) else it.plusNumber(
                payload.size.toLong(),
                Int.SIZE_BYTES,
            )
        }.let {
            if (il) it.plus(id.size.toByte()) else it
        }.plus(type).plus(id).plus(payload)
    }

    public data object Empty :
        NdefRecord(Tnf.Empty.value, Type.Empty.value, emptyByteArray(), emptyByteArray())

    public class Uri(id: ByteArray, public val uri: String) :
        NdefRecord(Tnf.Uri.value, Type.Uri.value, id, uri.encodeToByteArray())

    // TODO Android Application Record is a specific implementation of External. Should we add it in
    //  the Android package? Or here? Or ignore it?
    public class External(
        public val domain: String,
        public val extType: String,
        public val data: ByteArray
    ) : NdefRecord(
        Tnf.Ext.value,
        domain.encodeToByteArray().plus(':'.code.toByte()).plus(extType.encodeToByteArray()),
        byteArrayOf(),
        data,
    )

    public class Mime(id: ByteArray, public val mimeType: String, public val data: ByteArray) :
        NdefRecord(Tnf.Mime.value, mimeType.encodeToByteArray(), id, data)

    public class Unknown(id: ByteArray, payload: ByteArray) :
        NdefRecord(Tnf.Unknown.value, emptyByteArray(), id, payload)

    public sealed class WellKnown(public val rtd: ByteArray, id: ByteArray, payload: ByteArray) :
        NdefRecord(Tnf.WellKnown.value, rtd, id, payload) {

        public class AlternativeCarrier(
            id: ByteArray,
            public val powerState: PowerState,
            public val dataReference: String,
            public val auxiliaryDataReferences: List<String>,
        ) :
            WellKnown(
                Rtd.AltCarrier.value, id, byteArrayOf(powerState.value and 0x07)
                    .plus(dataReference.length.toByte())
                    .plus(dataReference.encodeToByteArray())
                    .plus(auxiliaryDataReferences.size.toByte())
                    .let {
                        auxiliaryDataReferences.fold(emptyByteArray()) { acc, str ->
                            acc.plus(str.length.toByte()).plus(str.encodeToByteArray())
                        }
                    }.plus(0x00)
            ) {
            public enum class PowerState(public val value: Byte) {
                Inactive(0x00), Active(0x01), Activating(0x02), Unknown(0x03)
            }

            internal companion object {
                internal fun from(id: ByteArray, payload: ByteArray): AlternativeCarrier {
                    var index = 0
                    val powerState =
                        payload[index++].let { PowerState.entries.first { state -> state.value == it } }
                    val dataReferenceLength = payload[index++].toPositiveInt()
                    val dataReference =
                        ByteArray(dataReferenceLength) { payload[index++] }.decodeToString()
                    val auxiliaryDataReferencesLength = payload[index].toPositiveInt()
                    var auxDataIndex = 0
                    val auxiliaryDataReferences = mutableListOf<String>()
                    while (auxDataIndex < auxiliaryDataReferencesLength) {
                        val auxDataLen = payload[index + auxDataIndex++].toPositiveInt()
                        auxiliaryDataReferences.add(ByteArray(auxDataLen) { payload[index + auxDataIndex++] }.decodeToString())
                    }

                    return AlternativeCarrier(
                        id,
                        powerState,
                        dataReference,
                        auxiliaryDataReferences,
                    )
                }
            }
        }

        public sealed class HandoverCarrier(
            id: ByteArray,
            public val carrierTypeFormat: Short,
            public val data: ByteArray,
            encoded: ByteArray,
        ) : WellKnown(Rtd.HandoverCarrier.value, id, run {
            byteArrayOf(carrierTypeFormat.toByte() and 0x07).plus(encoded.size.toByte())
                .plus(encoded).plus(data)
        }) {
//            protected abstract val encoded: ByteArray

            internal companion object {
                internal fun from(id: ByteArray, payload: ByteArray): HandoverCarrier {
                    val carrierTypeFormat =
                        (payload[0] and 0x07).toShort().let { Tnf.getByValue(it) }

                    val carrierTypeLength = payload[1].toPositiveInt()

                    val carrierType: ByteArray =
                        payload.sliceArray(IntRange(2, 2 + carrierTypeLength))

                    val data = payload.sliceArray(IntRange(2 + carrierTypeLength, payload.size - 1))

                    return when (carrierTypeFormat) {
                        Tnf.WellKnown -> {
                            val record = NdefMessage.from(carrierType).records.single()
                                .takeIf { it is NdefRecord.WellKnown }
                                ?: throw IllegalArgumentException("record is not WellKnown")
                            WellKnown(id, record as NdefRecord.WellKnown, data)
                        }

                        Tnf.Mime -> Mime(id, carrierType.decodeToString(), data)

                        Tnf.Uri -> Uri(id, carrierType.decodeToString(), data)

                        Tnf.Ext -> {
                            val record = NdefMessage.from(carrierType).records.single()
                                .takeIf { it is NdefRecord.External }
                                ?: throw IllegalArgumentException("record is not External")
                            External(id, record as NdefRecord.External, data)
                        }

                        else -> throw IllegalArgumentException("carrier type format is not one of WellKnown, Mime, Uri, or External")
                    }
                }
            }

            public class WellKnown(
                id: ByteArray,
                public val carrierType: NdefRecord.WellKnown,
                data: ByteArray
            ) : HandoverCarrier(
                id,
                Tnf.WellKnown.value,
                data,
                NdefMessage(listOf(carrierType)).toByteArray(),
            ) {
//                override val encoded: ByteArray = NdefMessage(listOf(carrierType)).toByteArray()
            }

            public class Mime(id: ByteArray, public val carrierType: String, data: ByteArray) :
                HandoverCarrier(id, Tnf.Mime.value, data, carrierType.encodeToByteArray()) {
//                override val encoded: ByteArray = carrierType.encodeToByteArray()
            }

            public class Uri(id: ByteArray, public val carrierType: String, data: ByteArray) :
                HandoverCarrier(id, Tnf.Uri.value, data, carrierType.encodeToByteArray()) {
//                override val encoded: ByteArray = carrierType.encodeToByteArray()
            }

            public class External(
                id: ByteArray,
                public val carrierType: NdefRecord.External,
                data: ByteArray
            ) : HandoverCarrier(
                id,
                Tnf.Ext.value,
                data,
                NdefMessage(listOf(carrierType)).toByteArray(),
            ) {
//                override val encoded: ByteArray = NdefMessage(listOf(carrierType)).toByteArray()
            }
        }

        public class CollisionResolution(id: ByteArray, public val randomNumber: Short) :
            WellKnown(
                Rtd.CollisionResolution.value,
                id,
                byteArrayOfNumber(randomNumber.toLong(), Short.SIZE_BYTES),
            )

        public class HandoverRequest(
            id: ByteArray,
            public val majorVersion: Byte,
            public val minorVersion: Byte,
            public val collisionResolution: CollisionResolution,
            public val alternativeCarriers: List<AlternativeCarrier>,
        ) : WellKnown(
            Rtd.HandoverRequest.value, id,
            byteArrayOf((majorVersion.toInt() shl 4 or minorVersion.toInt()).toByte()).plus(
                NdefMessage(listOf(collisionResolution).plus(alternativeCarriers)).toByteArray()
            )
        ) {
            internal companion object {
                internal fun from(id: ByteArray, payload: ByteArray): HandoverRequest {
                    val version = payload[0]
                    val majorVersion = (version.toInt() shr 4) and 0x0F
                    val minorVersion = version and 0x0F

                    val records = parse(payload.drop(1).toByteArray())

                    val collisionResolution = records
                        .first()
                        .takeIf {
                            it.tnf == Tnf.WellKnown.value &&
                                    it.type.contentEquals(Rtd.CollisionResolution.value)
                        } as? CollisionResolution
                        ?: throw IllegalArgumentException("first record of a HandoverRequest must be a CollisionResolution")

                    val alternativeCarriers = records.drop(1)
                        .takeIf { it.all { it.type.contentEquals(Rtd.AltCarrier.value) } }
                        ?.map { it as AlternativeCarrier }
                        ?: throw IllegalArgumentException("a HandoverRequest must contain exactly one CollisionResolution followed by one or more AlternativeCarriers it can't contain any other NdefRecord")

                    return HandoverRequest(
                        id,
                        majorVersion.toByte(),
                        minorVersion,
                        collisionResolution,
                        alternativeCarriers,
                    )
                }
            }
        }

        public class HandoverSelect(
            id: ByteArray,
            public val majorVersion: Byte,
            public val minorVersion: Byte,
            public val alternativeCarriers: List<AlternativeCarrier>,
            public val errorRecord: Error,
        ) : WellKnown(
            Rtd.HandoverSelect.value, id,
            byteArrayOf((majorVersion.toInt() shl 4 or minorVersion.toInt()).toByte()).plus(
                NdefMessage(alternativeCarriers.plus(errorRecord)).toByteArray()
            )
        ) {
            internal companion object {
                internal fun from(id: ByteArray, payload: ByteArray): HandoverSelect {
                    val version = payload[0]
                    val majorVersion = (version.toInt() shr 4) and 0x0F
                    val minorVersion = version and 0x0F

                    val records = parse(payload.drop(1).toByteArray())

                    val alternativeCarriers = records.dropLast(1)
                        .takeIf { it.all { it.type.contentEquals(Rtd.AltCarrier.value) } }
                        ?.map { it as AlternativeCarrier }
                        ?: throw IllegalArgumentException("a HandoverSelect must contain one or more")

                    val errorRecord = records
                        .last()
                        .takeIf {
                            it.tnf == Tnf.WellKnown.value &&
                                    it.type.contentEquals(Rtd.Error.value)
                        } as? Error
                        ?: throw IllegalArgumentException("last record of a HandoverSelect must be an ErrorRecord")


                    return HandoverSelect(
                        id,
                        majorVersion.toByte(),
                        minorVersion,
                        alternativeCarriers,
                        errorRecord,
                    )
                }
            }
        }

        /**
         *
         * @property data an `Int` if reason is `PermanentMemoryConstraints` if not a `Short` indicating wait
         * time before retrying.
         */
        public class Error(id: ByteArray, public val reason: Reason, public val data: Int) :
            WellKnown(
                Rtd.Error.value,
                id, byteArrayOf(reason.value).let {
                    if (reason != Reason.PermanentMemoryConstraints) it.plus(data.toByte())
                    else it.plusNumber(data.toLong(), Int.SIZE_BYTES)
                }
            ) {

            internal companion object {
                internal fun from(id: ByteArray, payload: ByteArray): Error {

                    val reason =
                        payload[0].let { value -> Reason.entries.first { value == it.value } }

                    val data: Long =
                        if (reason == Reason.PermanentMemoryConstraints) payload.getNumber(
                            1,
                            Int.SIZE_BYTES
                        )
                        else payload.getNumber(1, 1) and 0xFF

                    return Error(
                        id,
                        reason,
                        data.toInt(),
                    )
                }
            }

            public enum class Reason(public val value: Byte) {
                TemporaryMemoryConstraints(0x01),
                PermanentMemoryConstraints(0x02),
                CarrierSpecificConstraints(0x03);
            }
        }

        public class SmartPoster(id: ByteArray, public val records: List<WellKnown>) :
            WellKnown(Rtd.SmartPoster.value, id, NdefMessage(records).toByteArray()) {
            internal companion object {
                internal fun from(id: ByteArray, payload: ByteArray): SmartPoster {
                    val records = parse(payload)
                        .map {
                            if (it.tnf != Tnf.WellKnown.value) throw IllegalArgumentException("the payload must contain an NdefMessage with WellKnown records")
                            it as WellKnown
                        }
                    return SmartPoster(id, records)
                }
            }
        }

        /**
         *
         * @property languageCode is the locale of the language. It can contain a country part so both
         * `en` and `en-US` are valid.
         */
        public class Text(
            id: ByteArray,
            public val languageCode: String,
            public val encoding: Encoding,
            public val text: String,
        ) :
            WellKnown(Rtd.Text.value, id, run {
                val languageCodeBytes = languageCode.encodeToByteArray()
                languageCodeBytes.decodeToString()
                val status: Byte = (languageCodeBytes.size.toByte() or encoding.value)
                byteArrayOf(status).plus(languageCodeBytes).plus(
                    when (encoding) {
                        Encoding.UTF_8 -> text.encodeAsUtf8ByteArray()
                        Encoding.UTF_16 -> text.encodeAsUtf16ByteArray()
                    }
                )
            }) {

            public enum class Encoding(public val value: Byte) {
                UTF_8(0x00), UTF_16(0x80.toByte()),
            }

            internal companion object {
                private const val LANGUAGE_CODE_MASK = 0x1F
                private const val TEXT_ENCODING_MASK = 0x1F

                internal fun from(id: ByteArray, payload: ByteArray): Text {
                    val status = payload[0]

                    val encoding =
                        (status.toInt() and TEXT_ENCODING_MASK).toByte().let { encodingValue ->
                            Encoding.entries.first { encodingValue == it.value }
                        }

                    val languageCodeLength = (status.toInt() and LANGUAGE_CODE_MASK)
                    val languageCode =
                        payload.sliceArray(IntRange(1, languageCodeLength)).decodeToString()

                    val text =
                        payload.sliceArray(IntRange(1 + languageCodeLength, payload.size - 1))
                            .decodeToString()

                    return Text(
                        id,
                        languageCode,
                        encoding,
                        text,
                    )
                }
            }
        }

        public data class Uri(public val uri: String) : WellKnown(
            Rtd.Uri.value,
            emptyByteArray(),
            payload = run {
                val uriPrefixIndex =
                    URI_PREFIX_MAP
                        .drop(1)
                        .indexOfFirst { uri.startsWith(it) }
                        .takeIf { it >= 0 } ?: 0
                byteArrayOf(uriPrefixIndex.toByte())
                    .plus(uri.substring(URI_PREFIX_MAP[uriPrefixIndex].length).encodeToByteArray())
            }
        ) {
            public companion object {

                /**
                 * NFC Forum "URI Record Type Definition"
                 *
                 * This is a mapping of "URI Identifier Codes" to URI string prefixes,
                 * per section 3.2.2 of the NFC Forum URI Record Type Definition document.
                 */
                public val URI_PREFIX_MAP: Array<String> = arrayOf(
                    "",  // 0x00
                    "http://www.",  // 0x01
                    "https://www.",  // 0x02
                    "http://",  // 0x03
                    "https://",  // 0x04
                    "tel:",  // 0x05
                    "mailto:",  // 0x06
                    "ftp://anonymous:anonymous@",  // 0x07
                    "ftp://ftp.",  // 0x08
                    "ftps://",  // 0x09
                    "sftp://",  // 0x0A
                    "smb://",  // 0x0B
                    "nfs://",  // 0x0C
                    "ftp://",  // 0x0D
                    "dav://",  // 0x0E
                    "news:",  // 0x0F
                    "telnet://",  // 0x10
                    "imap:",  // 0x11
                    "rtsp://",  // 0x12
                    "urn:",  // 0x13
                    "pop:",  // 0x14
                    "sip:",  // 0x15
                    "sips:",  // 0x16
                    "tftp:",  // 0x17
                    "btspp://",  // 0x18
                    "btl2cap://",  // 0x19
                    "btgoep://",  // 0x1A
                    "tcpobex://",  // 0x1B
                    "irdaobex://",  // 0x1C
                    "file://",  // 0x1D
                    "urn:epc:id:",  // 0x1E
                    "urn:epc:tag:",  // 0x1F
                    "urn:epc:pat:",  // 0x20
                    "urn:epc:raw:",  // 0x21
                    "urn:epc:",  // 0x22
                )
            }
        }
    }

    public enum class Tnf(public val value: Short) {
        Empty(0x00),
        WellKnown(0x01),
        Mime(0x02),
        Uri(0x03),
        Ext(0x04),
        Unknown(0x05),
        Unchanged(0x06),
        Reserved(0x07);

        internal companion object : EnumWithValue<Tnf, Short> {
            override val Tnf.value: Short
                get() = value
        }
    }

    public enum class Type(public val value: ByteArray) {
        Empty(emptyByteArray()),
        Uri(byteArrayOf('U'.code.toByte())),
    }

    public enum class Rtd(public val value: ByteArray) {
        Text(byteArrayOf(0x54)),
        Uri(byteArrayOf(0x55)),
        SmartPoster(byteArrayOf(0x53, 0x70)),
        AltCarrier(byteArrayOf(0x61, 0x63)),
        HandoverCarrier(byteArrayOf(0x48, 0x63)),
        HandoverRequest(byteArrayOf(0x48, 0x72)),
        HandoverSelect(byteArrayOf(0x48, 0x73)),
        CollisionResolution(byteArrayOf('c'.code.toByte(), 'r'.code.toByte())),
        Error(byteArrayOf('e'.code.toByte(), 'r'.code.toByte(), 'r'.code.toByte())),
    }
}
