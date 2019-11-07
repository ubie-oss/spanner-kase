package app.ubie.spannerkase

class MigrationDataResource(
    private val prefix: String,
    private val name: String,
    private val classLoader: ClassLoader
) : MigrationData {
    override fun getSql(): String {
        return classLoader.getResourceAsStream("$prefix/$name").bufferedReader().use { it.readText() }
    }

    override fun getName(): String {
        return name
    }
}
