package utils

internal actual fun String.encodeAsUtf16ByteArray(): ByteArray {
    return this.byteInputStream(Charsets.UTF_16).readBytes()
}

internal actual fun ByteArray.decodeFromUtf16ByteArray(): String {
    return String(this, Charsets.UTF_16)
}
