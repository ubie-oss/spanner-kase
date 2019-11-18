package app.ubie.spannerkase.internal

import java.time.LocalDateTime

internal data class SchemeHistory(
    val installedRank: Long,
    val version: Long,
    val script: String,
    val checksum: Long,
    val installedOn: LocalDateTime
)
