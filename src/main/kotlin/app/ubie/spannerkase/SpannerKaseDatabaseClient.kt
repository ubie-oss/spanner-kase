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
            CREATE TABLE SchemeHistory (
                InstalledRank INT64 NOT NULL,
                Version INT64 NOT NULL,
                Script STRING(1000) NOT NULL,
                Checksum INT64 NOT NULL,
                InstalledOn TIMESTAMP NOT NULL
            ) PRIMARY KEY (InstalledRank)
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
                InstalledRank, Version, Script, Checksum, InstalledOn
            FROM
                SchemeHistory
            ORDER BY InstalledRank
            """.trimIndent()
        val statement = Statement.of(sql)
        return databaseClient.singleUse().executeQuery(statement).asSequence().map {
            SchemeHistory(
                installedRank = it.getLong("InstalledRank"),
                version = it.getLong("Version"),
                script = it.getString("Script"),
                checksum = it.getLong("Checksum"),
                installedOn = it.getTimestamp("InstalledOn").toSqlTimestamp().toLocalDateTime()
            )
        }.toList()
    }

    internal fun insertSchemeHistory(schemeHistory: SchemeHistory) {
        val mutation = insert("SchemeHistory") {
            it["InstalledRank"] = schemeHistory.installedRank
            it["Version"] = schemeHistory.version
            it["Script"] = schemeHistory.script
            it["Checksum"] = schemeHistory.checksum
            it["InstalledOn"] = schemeHistory.installedOn.toTimestamp()
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
              t.table_catalog = '' and t.table_schema = '' and t.table_name = 'SchemeHistory'
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
