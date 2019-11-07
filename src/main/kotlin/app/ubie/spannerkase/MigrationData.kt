package app.ubie.spannerkase

import com.google.common.hash.Hashing
import java.nio.charset.Charset

interface MigrationData {
    fun getName(): String
    fun getSql(): String
    fun getVersion(): Long {
        val name = getName()
        check(name.startsWith("V")) { "invalid file name: $name" }
        return name.split("__")[0].drop(1).toLongOrNull() ?: throw IllegalStateException("invalid file name: $name")
    }
    fun getChecksum(): Long {
        return Hashing.sha256().hashString(getSql(), Charset.defaultCharset()).asLong()
    }
}
