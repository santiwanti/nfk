package utils

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.getBytes
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.utf16

@OptIn(ExperimentalForeignApi::class)
internal actual fun String.encodeAsUtf16ByteArray(): ByteArray {
    return this.utf16.getBytes()
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun ByteArray.decodeFromUtf16ByteArray(): String {
    return ShortArray(this.size / 2) { i ->
        val pos = i * 2
        ((this[pos].toUByte().toInt() shl 8) + this[pos + 1].toUByte().toInt()).toShort()
    }.usePinned {
        it.addressOf(0).toKStringFromUtf16()
    }
}