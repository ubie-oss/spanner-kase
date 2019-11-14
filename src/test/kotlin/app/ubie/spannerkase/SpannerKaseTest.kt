package app.ubie.spannerkase

import app.ubie.spannerkase.internal.MigrationData
import app.ubie.spannerkase.internal.MigrationDataScanner
import app.ubie.spannerkase.internal.SchemeHistory
import app.ubie.spannerkase.internal.SchemeHistoryRepository
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

internal class SpannerKaseTest {

    @Nested
    inner class migrate {
        @Test
        fun `migration files is empty`() {
            val schemeHistoryRepository: SchemeHistoryRepository = mockk {}
            val configure: SpannerKase.Configure = mockk {
                every { databaseClient } returns mockk()
                every { createSchemeHistoryRepository() } returns schemeHistoryRepository
                every { createMigrationDataScanner() } returns mockk {
                    every { scan() } returns emptyList()
                }
            }

            val spannerKase = SpannerKase(configure)

            spannerKase.migrate()
            verify(exactly = 0) { schemeHistoryRepository.currentVersion() }
            verify(exactly = 0) { schemeHistoryRepository.versionHistories() }
        }

        @Test
        fun `first migration`() {
            val schemeHistoryRepository: SchemeHistoryRepository = mockk {
                every { versionHistories() } returns emptyList()
                every { currentVersion() } returns null
            }
            val migrationDataScanner: MigrationDataScanner = mockk {
                every { scan() } returns listOf(
                    createMockkMigrationData(1, "aaa", "aaa", 1),
                    createMockkMigrationData(2, "bbb", "bbb", 2)
                )
            }
            val mockDatabaseClient: SpannerKaseDatabaseClient = mockk {
                every { executeSql(any()) } just Runs
                every { insertSchemeHistory(any()) } just Runs
            }
            val configure: SpannerKase.Configure = mockk {
                every { databaseClient } returns mockDatabaseClient
                every { createSchemeHistoryRepository() } returns schemeHistoryRepository
                every { createMigrationDataScanner() } returns migrationDataScanner
            }
            val spannerKase = SpannerKase(configure)
            spannerKase.migrate()

            verify(exactly = 2) { mockDatabaseClient.executeSql(any()) }
        }

        @Test
        fun `process new migration files`() {
            val schemaHistory = SchemeHistory(3, 3, "aaa", 3, LocalDateTime.now())
            val schemeHistoryRepository: SchemeHistoryRepository = mockk {
                every { versionHistories() } returns listOf(
                    SchemeHistory(1, 1, "aaa", 1, LocalDateTime.now()),
                    SchemeHistory(2, 2, "bbb", 2, LocalDateTime.now()),
                    schemaHistory
                )
                every { currentVersion() } returns schemaHistory
            }
            val migrationDataScanner: MigrationDataScanner = mockk {
                every { scan() } returns listOf(
                    createMockkMigrationData(1, "aaa", "aaa", 1),
                    createMockkMigrationData(2, "bbb", "bbb", 2),
                    createMockkMigrationData(3, "ccc", "ccc", 3),
                    createMockkMigrationData(4, "ddd", "ddd", 4),
                    createMockkMigrationData(5, "eee", "eee", 5)
                )
            }
            val mockDatabaseClient: SpannerKaseDatabaseClient = mockk {
                every { executeSql(any()) } just Runs
                every { insertSchemeHistory(any()) } just Runs
            }
            val configure: SpannerKase.Configure = mockk {
                every { databaseClient } returns mockDatabaseClient
                every { createSchemeHistoryRepository() } returns schemeHistoryRepository
                every { createMigrationDataScanner() } returns migrationDataScanner
            }
            val spannerKase = SpannerKase(configure)
            spannerKase.migrate()

            verify { mockDatabaseClient.executeSql(eq("ddd")) }
            verify { mockDatabaseClient.executeSql(eq("eee")) }
        }

        @Test
        fun `checksum is changed`() {
            val schemaHistory1 = SchemeHistory(1, 1, "zzz", 1, LocalDateTime.now())
            val schemaHistory2 = SchemeHistory(2, 2, "zzz", 1, LocalDateTime.now())
            val schemaHistory3 = SchemeHistory(3, 3, "aaa", 3, LocalDateTime.now())
            val schemeHistoryRepository: SchemeHistoryRepository = mockk {
                every { versionHistories() } returns listOf(schemaHistory1, schemaHistory2, schemaHistory3)
                every { currentVersion() } returns schemaHistory3
            }
            val migrationDataScanner: MigrationDataScanner = mockk {
                every { scan() } returns listOf(
                    createMockkMigrationData(1, "aaa", "aaa", 1),
                    createMockkMigrationData(2, "bbb", "bbb", 2),
                    createMockkMigrationData(3, "ccc", "ccc", 3),
                    createMockkMigrationData(4, "ddd", "ddd", 4),
                    createMockkMigrationData(5, "eee", "eee", 5)
                )
            }
            val mockDatabaseClient: SpannerKaseDatabaseClient = mockk {
                every { executeSql(any()) } just Runs
                every { insertSchemeHistory(any()) } just Runs
            }
            val configure: SpannerKase.Configure = mockk {
                every { databaseClient } returns mockDatabaseClient
                every { createSchemeHistoryRepository() } returns schemeHistoryRepository
                every { createMigrationDataScanner() } returns migrationDataScanner
            }
            val spannerKase = SpannerKase(configure)
            val exception = assertThrows<Exception> {
                spannerKase.migrate()
            }
            assertThat(exception.message).isEqualTo("checksum is different. bbb")
        }

        @Test
        fun `find a migration file that older than current version`() {
            val schemaHistory1 = SchemeHistory(2, 2, "bbb", 2, LocalDateTime.now())
            val schemaHistory2 = SchemeHistory(3, 3, "ccc", 3, LocalDateTime.now())
            val schemeHistoryRepository: SchemeHistoryRepository = mockk {
                every { versionHistories() } returns listOf(schemaHistory1, schemaHistory2)
                every { currentVersion() } returns schemaHistory2
            }
            val migrationDataScanner: MigrationDataScanner = mockk {
                every { scan() } returns listOf(
                    createMockkMigrationData(1, "aaa", "aaa", 1),
                    createMockkMigrationData(2, "bbb", "bbb", 2),
                    createMockkMigrationData(3, "ccc", "ccc", 3),
                    createMockkMigrationData(4, "ddd", "ddd", 4),
                    createMockkMigrationData(5, "eee", "eee", 5)
                )
            }
            val mockDatabaseClient: SpannerKaseDatabaseClient = mockk {
                every { executeSql(any()) } just Runs
                every { insertSchemeHistory(any()) } just Runs
            }
            val configure: SpannerKase.Configure = mockk {
                every { databaseClient } returns mockDatabaseClient
                every { createSchemeHistoryRepository() } returns schemeHistoryRepository
                every { createMigrationDataScanner() } returns migrationDataScanner
            }
            val spannerKase = SpannerKase(configure)
            val exception = assertThrows<Exception> {
                spannerKase.migrate()
            }
            assertThat(exception.message).isEqualTo("find a migration file that older than current version. aaa")
        }
    }

    fun createMockkMigrationData(mockVersion: Long, mockName: String, mockSql: String, mockChecksum: Long): MigrationData {
        return mockk {
            every { version } returns mockVersion
            every { name } returns mockName
            every { sql } returns mockSql
            every { checksum } returns mockChecksum
        }
    }
}
