package app.ubie.spannerkase

import app.ubie.spannerkase.internal.MigrationData
import app.ubie.spannerkase.internal.MigrationDataScanner
import app.ubie.spannerkase.internal.SchemeHistory
import app.ubie.spannerkase.internal.SchemeHistoryRepository
import java.time.LocalDateTime

class SpannerKase(
    val configure: Configure
) {
    class Configure(
        val databaseClient: SpannerKaseDatabaseClient,
        private val classLoader: ClassLoader,
        private val path: String
    ) {
        internal fun createSchemeHistoryRepository(): SchemeHistoryRepository {
            return SchemeHistoryRepository(databaseClient).apply {
                createSchemeHistory()
            }
        }

        internal fun createMigrationDataScanner(): MigrationDataScanner {
            return MigrationDataScanner(classLoader, path)
        }
    }

    fun migrate() {
        val databaseClient = configure.databaseClient
        val migrationDataScanner = configure.createMigrationDataScanner()
        val schemeHistoryRepository = configure.createSchemeHistoryRepository()
        val migrationDataList = migrationDataScanner.scan()
        if (migrationDataList.isEmpty()) {
            return
        }

        // verify checksum
        val versionHistories = schemeHistoryRepository.versionHistories()
        findInvalidChecksumMigrationData(migrationDataList, versionHistories)?.let {
            throw IllegalStateException("checksum is different. ${it.name}")
        }

        // verify versions
        val currentVersion = schemeHistoryRepository.currentVersion()?.version ?: 0
        findInvalidVersionMigrationData(migrationDataList, versionHistories, currentVersion)?.let {
            throw IllegalStateException("find a migration file that older than current version. ${it.name}")
        }

        // get new migration files
        migrationDataList
            .filter { it.version > currentVersion }
            .forEach { migrationData ->
                val sql = migrationData.sql
                databaseClient.executeSql(sql)
                databaseClient.insertSchemeHistory(
                    SchemeHistory(
                        migrationData.version,
                        migrationData.version,
                        migrationData.name,
                        migrationData.checksum,
                        LocalDateTime.now()
                    )
                )
            }
    }


    private fun findInvalidChecksumMigrationData(
        migrationDataList: List<MigrationData>,
        versionHistories: List<SchemeHistory>
    ): MigrationData? {
        return versionHistories.asSequence()
            .mapNotNull { history ->
                migrationDataList.find { it.version == history.version && it.checksum != history.checksum }
            }
            .firstOrNull()
    }

    private fun findInvalidVersionMigrationData(
        migrationDataList: List<MigrationData>,
        versionHistories: List<SchemeHistory>,
        currentVersion: Long
    ): MigrationData? {
        return migrationDataList.find { migrationData ->
            migrationData.version < currentVersion && versionHistories.any { migrationData.version == it.version }.not()
        }
    }
}
