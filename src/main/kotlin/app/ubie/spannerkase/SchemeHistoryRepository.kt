package app.ubie.spannerkase

class SchemeHistoryRepository(private val client: SpannerKaseDatabaseClient) {
    init {
         client.createSchemeHistory()
    }

    fun currentVersion(): SchemeHistory? {
        return client.allSchemeHistory().lastOrNull()
    }
    fun versionHistories(): List<SchemeHistory> {
        return client.allSchemeHistory()
    }
}
