package app.ubie.spannerkase.internal

import com.google.common.hash.Hashing
import java.nio.charset.Charset

internal abstract class MigrationData {
    abstract val name: String
    abstract val sql: String

    val version: Long
        get() {
            check(name.startsWith("V")) { "invalid file name: $name" }
            return name.split("__")[0].drop(1).toLongOrNull() ?: throw IllegalStateException("invalid file name: $name")
        }
    val checksum: Long
        get() {
            return Hashing.sha256().hashString(sql, Charset.defaultCharset()).asLong()
        }
}
