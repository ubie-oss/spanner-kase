package app.ubie.spannerkase.internal

import app.ubie.spannerkase.SpannerKaseDatabaseClient

internal class SchemeHistoryRepository(private val client: SpannerKaseDatabaseClient) {
    fun createSchemeHistory() {
         client.createSchemeHistory()
    }
    fun currentVersion(): SchemeHistory? {
        return client.allSchemeHistory().lastOrNull()
    }
    fun versionHistories(): List<SchemeHistory> {
        return client.allSchemeHistory()
    }
}
