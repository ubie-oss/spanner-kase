package app.ubie.spannerkase

import com.google.common.hash.Hashing
import java.nio.charset.Charset

abstract class MigrationData {
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
