package utils

// toInt and 0xFF is used to ensure that the value is positive
/**
 * Turns the byte into a positive int. something like 0x81 is -127, after returning
 * from this function it would be positive 129
 */
internal fun Byte.toPositiveInt(): Int {
    return this.toInt() and 0xFF
}