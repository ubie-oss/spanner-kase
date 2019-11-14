package app.ubie.spannerkase

import sun.net.www.protocol.jar.JarURLConnection
import java.io.File
import java.net.URL
import java.util.jar.JarFile

class MigrationDataScanner(private val classLoader: ClassLoader, private val path: String) {
    fun scan(): List<MigrationData> {
        return classLoader.getResources(path).toList().flatMap { url ->
            when (url.protocol) {
                "file" -> {
                    fileScan(url)
                }
                "jar" -> {
                    val connection = url.openConnection()
                    jarScan((connection as JarURLConnection).jarFile, path, classLoader)
                }
                else -> {
                    emptyList()
                }
            }
        }.sortedBy(MigrationData::version)
    }

    private fun fileScan(url: URL): List<MigrationData> {
        val file = File(url.file)
        return file.listFiles()?.toList()?.map { MigrationDataFile(it) } ?: emptyList()
    }

    private fun jarScan(jarFile: JarFile, path: String, classLoader: ClassLoader): List<MigrationData> {
        val prefix = "$path/"
        return jarFile.entries().toList().filter {
            it.name.startsWith(prefix) && prefix.length < it.name.length
        }.map {
            MigrationDataResource(
                path,
                it.name.drop(prefix.length),
                classLoader
            )
        }
    }
}
