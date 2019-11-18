package app.ubie.spannerkase

import app.ubie.spannerkase.internal.SchemeHistory
import app.ubie.spannerkase.internal.insert
import app.ubie.spannerkase.internal.set
import app.ubie.spannerkase.internal.toTimestamp
import com.google.cloud.spanner.DatabaseAdminClient
import com.google.cloud.spanner.DatabaseClient
import com.google.cloud.spanner.ResultSet
import com.google.cloud.spanner.Statement

class SpannerKaseDatabaseClient(
    private val instanceId: String,
    private val databaseId: String,
    private val databaseAdminClient: DatabaseAdminClient,
    private val databaseClient: DatabaseClient
) {
    fun executeSql(sql: String) {
        val statements = sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        val ddl = statements.filter {
            val value = it.toLowerCase()
            !value.startsWith("insert") && !value.startsWith("update") && !value.startsWith("delete")
        }
        val update = statements - ddl
        if (ddl.isNotEmpty()) {
            databaseAdminClient.updateDatabaseDdl(
                instanceId,
                databaseId,
                ddl,
                null
            ).get()
        }
        if (update.isNotEmpty()) {
            databaseClient.readWriteTransaction().run { transaction ->
                update.forEach { sql ->
                    transaction.executeUpdate(Statement.of(sql))
                }
            }
        }
    }

    fun createSchemeHistory() {
        if (alreadyInitialized()) {
            return
        }
        val sql =
            //language=SQL
            """
            CREATE TABLE scheme_history (
                installed_rank INT64 NOT NULL,
                version INT64 NOT NULL,
                script STRING(1000) NOT NULL,
                checksum INT64 NOT NULL,
                installed_on TIMESTAMP NOT NULL
            ) PRIMARY KEY (installed_rank)
            """.trimIndent()
        databaseAdminClient.updateDatabaseDdl(
            instanceId,
            databaseId,
            listOf(sql),
            null
        ).get()
    }

    internal fun allSchemeHistory(): List<SchemeHistory> {
        val sql =
            //language=SQL
            """
            SELECT
                installed_rank, version, script, checksum, installed_on
            FROM
                scheme_history
            ORDER BY installed_rank
            """.trimIndent()
        val statement = Statement.of(sql)
        return databaseClient.singleUse().executeQuery(statement).asSequence().map {
            SchemeHistory(
                installedRank = it.getLong("installed_rank"),
                version = it.getLong("version"),
                script = it.getString("script"),
                checksum = it.getLong("checksum"),
                installedOn = it.getTimestamp("installed_on").toSqlTimestamp().toLocalDateTime()
            )
        }.toList()
    }

    internal fun insertSchemeHistory(schemeHistory: SchemeHistory) {
        val mutation = insert("scheme_history") {
            it["installed_rank"] = schemeHistory.installedRank
            it["version"] = schemeHistory.version
            it["script"] = schemeHistory.script
            it["checksum"] = schemeHistory.checksum
            it["installed_on"] = schemeHistory.installedOn.toTimestamp()
        }
        databaseClient.write(listOf(mutation))
    }

    private fun alreadyInitialized(): Boolean {
        val sql = """
            SELECT
              count(0) as count
            FROM
              information_schema.tables AS t
            WHERE
              t.table_catalog = '' and t.table_schema = '' and t.table_name = 'scheme_history'
""".trimIndent()
        return databaseClient.singleUse().executeQuery(Statement.of(sql)).run {
            next()
            getLong("count") == 1L
        }
    }

    private fun ResultSet.asSequence(): Sequence<ResultSet> {
        return Sequence {
            object : Iterator<ResultSet> {
                override fun hasNext(): Boolean {
                    return this@asSequence.next()
                }

                override fun next(): ResultSet {
                    return this@asSequence
                }
            }
        }
    }
}
