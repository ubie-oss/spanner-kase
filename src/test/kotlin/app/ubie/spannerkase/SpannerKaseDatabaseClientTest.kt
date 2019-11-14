package app.ubie.spannerkase

import com.google.cloud.spanner.DatabaseAdminClient
import com.google.cloud.spanner.DatabaseClient
import com.google.cloud.spanner.TransactionContext
import com.google.cloud.spanner.TransactionRunner
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

internal class SpannerKaseDatabaseClientTest {

    @Nested
    inner class executeSql {
        @Test
        fun executeSql() {
            val slot = CapturingSlot<Iterable<String>>()
            val databaseAdminClient: DatabaseAdminClient = mockk {
                every {
                    updateDatabaseDdl(any(), any(), capture(slot), any())
                } returns mockk {
                    every {
                        get()
                    } returns mockk()
                }
            }
            val transactionContext: TransactionContext = mockk {
                every { executeUpdate(any()) } returns 1L
            }
            val databaseClient: DatabaseClient = mockk {
                every {
                    readWriteTransaction()
                } returns mockk {
                    every { run(any<TransactionRunner.TransactionCallable<Any>>()) } answers {
                        this.arg<TransactionRunner.TransactionCallable<Any>>(0).run(transactionContext)
                    }
                }
            }
            val client = SpannerKaseDatabaseClient(
                "instance",
                "database",
                databaseAdminClient,
                databaseClient
            )
            val sql = """
                CREATE TABLE user
                (
                    id STRING(36)  NOT NULL,
                    name             String(128) NOT NULL
                ) PRIMARY KEY (id);
                CREATE UNIQUE INDEX user_name_index ON user (name);
                
                CREATE TABLE user_type
                (
                    id Int64   NOT NULL,
                    name                String(128) NOT NULL
                ) PRIMARY KEY (id);
                CREATE UNIQUE INDEX user_type_name_index ON user_type (name);
                
                INSERT INTO user_type(id, name) Values
                 (1, "A"),
                 (2, "B"),
                 (3, "C"),;
                
                CREATE TABLE user_type_mapping
                (
                    user_id STRING(36)  NOT NULL,
                    user_type_id Int64   NOT NULL
                ) PRIMARY KEY (user_id, user_type_id),
                INTERLEAVE IN PARENT user ON DELETE CASCADE;
            """.trimIndent()
            client.executeSql(sql)

            assertThat(slot.captured.count()).isEqualTo(5)
            verify(exactly = 1) { databaseAdminClient.updateDatabaseDdl(any(), any(), any(), any()) }
            verify(exactly = 1) { databaseClient.readWriteTransaction() }
            verify(exactly = 1) { transactionContext.executeUpdate(any()) }
        }
    }
}
