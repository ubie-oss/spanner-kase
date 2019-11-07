package app.ubie.spannerkase

import java.time.LocalDateTime

data class SchemeHistory(
    val installedRank: Long,
    val version: Long,
    val script: String,
    val checksum: Long,
    val installedOn: LocalDateTime
)
