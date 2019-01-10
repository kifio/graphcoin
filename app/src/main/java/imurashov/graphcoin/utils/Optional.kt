package imurashov.graphcoin.utils

class Optional<T>(val value: T?) {

    fun getOrNull(): T? = value

    companion object {

        fun <T> of(value: T?): Optional<T> {
            return Optional(value)
        }
    }
}