import utils.emptyByteArray

public class NdefMessage(public val records: List<NdefRecord>) {

    // TODO could this be improved by not creating 20 different byteArrays?
    // TODO implement chunking if the message is too long. 2^32-1 size limit. This is not high prio.
    //  no way an NFC has such big payload is there?
    public fun toByteArray(): ByteArray {
        return records.foldIndexed(emptyByteArray()) { index, acc, record ->
            acc.plus(record.toByteArray(index == 0, index == records.lastIndex))
        }
    }

    public companion object {
        public fun from(data: ByteArray): NdefMessage {
            return NdefMessage(NdefRecord.parse(data))
        }
    }
}