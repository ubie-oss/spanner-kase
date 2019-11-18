package app.ubie.spannerkase.internal

import com.google.cloud.Timestamp
import com.google.cloud.spanner.Mutation
import java.time.LocalDateTime
import java.time.ZoneOffset

internal inline operator fun Mutation.WriteBuilder.set(key: String, value: Long) {
    set(key).to(value)
}

internal inline operator fun Mutation.WriteBuilder.set(key: String, value: String) {
    set(key).to(value)
}

internal inline operator fun Mutation.WriteBuilder.set(key: String, value: Timestamp) {
    set(key).to(value)
}

internal fun insert(tableName: String, builder: (Mutation.WriteBuilder) -> Unit): Mutation {
    return Mutation.newInsertBuilder(tableName)
        .apply {
            builder(this)
        }.build()
}

internal fun LocalDateTime.toTimestamp(): Timestamp {
    return Timestamp.of(java.sql.Timestamp.from(toInstant(ZoneOffset.UTC)))
}
