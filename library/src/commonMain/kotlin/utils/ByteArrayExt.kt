package utils

/**
 * This works for numbers with no decimals e.g. Short, Int, Long. For Short and Int turn them into
 * Long and call this function.
 */
internal fun ByteArray.plusNumber(value: Long, byteSize: Int): ByteArray {
    return this.plus(byteArrayOfNumber(value, byteSize))
}

/**
 * This works for numbers with no decimals e.g. Short, Int, Long. For Short and Int turn them into
 * Long and call this function.
 */
internal fun byteArrayOfNumber(value: Long, byteSize: Int): ByteArray {
    require(byteSize in 1..8)
    return ByteArray(byteSize) { i ->
        (value shr (i * 8) and 0xff).toByte()
    }
}

/**
 * Reads a number from the given ByteArray and returns it as a Long.
 */
internal fun ByteArray.getNumber(startPos: Int, byteSize: Int): Long {
    require(byteSize in 1..8)
    var result = 0L
    for (index in 0 until byteSize) {
        val newByte =
            if (index == 0) get(startPos + index).toInt() else get(startPos + index).toInt() and 0xFF
        result = result shl 8 + newByte
    }
    return result
}

internal fun emptyByteArray(): ByteArray = byteArrayOf()

@OptIn(ExperimentalStdlibApi::class)
public fun ByteArray.displayAsHex(): String {
    return this.toHexString(HexFormat.UpperCase)
        .toCharArray()
        .asIterable()
        .windowed(2, 2, false)
        .joinToString(separator = ":", prefix = "0x") { "${it.component1()}${it.component2()}" }
}
