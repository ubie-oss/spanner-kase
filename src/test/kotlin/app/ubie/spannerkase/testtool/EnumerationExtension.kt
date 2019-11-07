package app.ubie.spannerkase.testtool

import java.util.*
import kotlin.NoSuchElementException

fun <T> List<T>.toEnumeration(): Enumeration<T> {
    return object : Enumeration<T> {
        var count = 0

        override fun hasMoreElements(): Boolean {
            return this.count < size
        }

        override fun nextElement(): T {
            if (this.count < size) {
                return get(this.count++)
            }
            throw NoSuchElementException("List enumeration asked for more elements than present")
        }
    }
}
