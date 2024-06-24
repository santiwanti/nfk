package utils

internal interface EnumWithValue<T : Enum<T>, K> {
    val T.value: K
}

internal inline fun <reified T : Enum<T>, V> EnumWithValue<T, V>.getByValue(value: V): T {
    return enumValues<T>().first { it.value == value }
}
