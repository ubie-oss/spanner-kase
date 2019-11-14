package app.ubie.spannerkase.internal

internal class MigrationDataResource(
    private val prefix: String,
    override val name: String,
    private val classLoader: ClassLoader
) : MigrationData() {
    override val sql: String by lazy {
        classLoader.getResourceAsStream("$prefix/$name").bufferedReader().use { it.readText() }
    }
}
