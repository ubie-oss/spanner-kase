package app.ubie.spannerkase

import java.io.File

class MigrationDataFile(
    private val file: File
) : MigrationData() {
    override val sql: String by lazy {
        file.inputStream().bufferedReader().use { it.readText() }
    }
    override val name = file.name
}
