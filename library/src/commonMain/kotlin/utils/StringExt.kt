package utils

internal fun String.encodeAsUtf8ByteArray(): ByteArray {
    return encodeToByteArray()
}

internal expect fun String.encodeAsUtf16ByteArray(): ByteArray

internal fun ByteArray.decodeFromUtf8ByteArray(): String {
    return decodeToString()
}

internal expect fun ByteArray.decodeFromUtf16ByteArray(): String
