package app.ubie.spannerkase

import java.time.LocalDateTime

class SpannerKase(val schemeHistoryRepository: SchemeHistoryRepository, val migrationDataScanner: MigrationDataScanner) {
    fun migrate(databaseClient: SpannerKaseDatabaseClient) {
        val migrationDataList = migrationDataScanner.scan()
        if (migrationDataList.isEmpty()) {
            return
        }

        // verify checksum
        val versionHistories = schemeHistoryRepository.versionHistories()
        versionHistories.forEach { history ->
            val version = history.version
            migrationDataList.find { it.getVersion() == version }?.let { migrationData ->
                if (history.checksum != migrationData.getChecksum()) {
                    throw IllegalStateException("checksum is different. ${migrationData.getName()}")
                }
            }
        }

        val currentVersion = schemeHistoryRepository.currentVersion()?.version ?: 0
        // verify versions
        migrationDataList
            .filter { it.getVersion() < currentVersion }
            .forEach { migrationData ->
                versionHistories.find { migrationData.getVersion() == it.version }
                    ?: throw IllegalStateException("find a migration file that older than current version. ${migrationData.getName()}")
            }

        // get new migration files
        migrationDataList
            .filter { it.getVersion() > currentVersion }
            .forEach { migrationData ->
                val sql = migrationData.getSql()
                databaseClient.executeSql(sql)
                databaseClient.insertSchemeHistory(
                    SchemeHistory(
                        migrationData.getVersion(),
                        migrationData.getVersion(),
                        migrationData.getName(),
                        migrationData.getChecksum(),
                        LocalDateTime.now()
                    )
                )
            }
    }
}
