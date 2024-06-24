package utils

@OptIn(ExperimentalStdlibApi::class)
public fun Short.displayAsHex(): String {
    return this.toHexString(HexFormat.UpperCase)
        .toCharArray()
        .asIterable()
        .windowed(2, 2, false)
        .joinToString(":") { "0x${it.component1()}${it.component2()}" }
}