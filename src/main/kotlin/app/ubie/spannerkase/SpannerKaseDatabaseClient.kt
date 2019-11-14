package app.ubie.spannerkase

import app.ubie.spannerkase.internal.SchemeHistory
import com.google.cloud.spanner.DatabaseAdminClient
import com.google.cloud.spanner.Mutation
import com.google.cloud.spanner.ResultSet
import com.google.cloud.spanner.Statement
import java.sql.Timestamp
import java.time.ZoneOffset

class SpannerKaseDatabaseClient(
    private val instanceId: String,
    private val databaseId: String,
    private val databaseAdminClient: DatabaseAdminClient,
    private val databaseClient: com.google.cloud.spanner.DatabaseClient
) {
    fun executeSql(sql: String) {
        val statements = sql.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        val ddl = statements.filter {
            val value = it.toLowerCase()
            !value.startsWith("insert") && !value.startsWith("update") && !value.startsWith("delete")
        }
        val update = statements.filter {
            val value = it.toLowerCase()
            value.startsWith("insert") || value.startsWith("update") || value.startsWith("delete")
        }
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
"""
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
        val mutation = Mutation.newInsertBuilder("SchemeHistory")
            .set("InstalledRank")
            .to(schemeHistory.installedRank)
            .set("Version")
            .to(schemeHistory.version)
            .set("Script")
            .to(schemeHistory.script)
            .set("Checksum")
            .to(schemeHistory.checksum)
            .set("InstalledOn")
            .to(com.google.cloud.Timestamp.of(Timestamp.from(schemeHistory.installedOn.toInstant(ZoneOffset.UTC))))
            .build()
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
