package app.ubie.spannerkase

interface MigrationDataScanner {
    fun scan(): List<MigrationData>
}
