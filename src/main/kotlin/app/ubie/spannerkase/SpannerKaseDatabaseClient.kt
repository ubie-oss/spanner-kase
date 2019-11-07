package app.ubie.spannerkase

interface SpannerKaseDatabaseClient {
    fun executeSql(sql: String)
    fun createSchemeHistory()
    fun allSchemeHistory(): List<SchemeHistory>
    fun insertSchemeHistory(schemeHistory: SchemeHistory)
}
