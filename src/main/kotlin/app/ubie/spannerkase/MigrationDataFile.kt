package app.ubie.spannerkase

import java.io.File

class MigrationDataFile(
    private val file: File
) : MigrationData {

    override fun getSql(): String {
        return file.inputStream().bufferedReader().use { it.readText() }
    }

    override fun getName(): String {
        return file.name
    }
}
