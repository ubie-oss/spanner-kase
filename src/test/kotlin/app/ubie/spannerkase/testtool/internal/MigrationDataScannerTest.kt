package app.ubie.spannerkase.testtool.internal

import app.ubie.spannerkase.ClassLoaderMigrationDataScanner
import app.ubie.spannerkase.testtool.toEnumeration
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import sun.net.www.protocol.jar.JarURLConnection
import java.net.URL
import java.util.jar.JarEntry

internal class MigrationDataScannerTest {
    @Nested
    inner class sort {
        @Test
        fun `success`() {
            val prefix = "db/migration"
            val classLoader: ClassLoader = createDummyClassLoader(prefix, listOf("V1__valid.sql", "V2__valid.sql"))
            val migrationDataScanner = ClassLoaderMigrationDataScanner(classLoader, prefix)
            val resources = migrationDataScanner.scan()

            assertThat(resources.size).isEqualTo(2)
        }

        @Test
        fun `invalid file name, prefix V is not found`() {
            val prefix = "db/migration"
            val classLoader: ClassLoader = createDummyClassLoader(prefix, listOf("V1__valid.sql", "111__invalid.sql"))
            val migrationDataScanner = ClassLoaderMigrationDataScanner(classLoader, prefix)
            val exception = assertThrows<Exception> {
                migrationDataScanner.scan()
            }
            assertThat(exception.message).isEqualTo("invalid file name: 111__invalid.sql")
        }

        @Test
        fun `invalid file name, version is not number`() {
            val prefix = "db/migration"
            val classLoader: ClassLoader = createDummyClassLoader(prefix, listOf("V1__valid.sql", "Vaaa__invalid.sql"))
            val migrationDataScanner = ClassLoaderMigrationDataScanner(classLoader, prefix)
            val exception = assertThrows<Exception> {
                migrationDataScanner.scan()
            }
            assertThat(exception.message).isEqualTo("invalid file name: Vaaa__invalid.sql")
        }

        @Test
        fun `invalid file name, wrong separator`() {
            val prefix = "db/migration"
            val classLoader: ClassLoader = createDummyClassLoader(prefix, listOf("V1__valid.sql", "V100_invalid.sql"))
            val migrationDataScanner = ClassLoaderMigrationDataScanner(classLoader, prefix)
            val exception = assertThrows<Exception> {
                migrationDataScanner.scan()
            }
            assertThat(exception.message).isEqualTo("invalid file name: V100_invalid.sql")
        }
    }

    @Nested
    inner class scan {
        @Test
        fun scanFromTestEnvironment() {
            val migrationDataScanner =
                ClassLoaderMigrationDataScanner(this.javaClass.classLoader, "db/migration")
            val files = migrationDataScanner.scan()
            assertThat(files.size).isEqualTo(2)
            files.forEach {
                println(it.name)
                println(it.version)
                println(it.sql)
                println(it.checksum)
            }
        }
    }

    private fun createDummyClassLoader(
        prefix: String,
        fileNames: List<String>
    ): ClassLoader {
        return mockk {
            every { getResources(any()) } returns listOf(
                mockk<URL> {
                    every { protocol } returns "jar"
                    every { openConnection() } returns mockk<JarURLConnection> {
                        every { jarFile } returns mockk {
                            every { entries() } returns fileNames.map {
                                JarEntry("$prefix/$it")
                            }.toEnumeration()
                        }
                    }
                }
            ).toEnumeration()
        }
    }
}
