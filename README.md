# spanner-kase

spanner-kase is a schema version control library for spanner using Kotlin.

<img src="https://user-images.githubusercontent.com/749051/68369421-a04a8700-017d-11ea-802f-0e5895519757.jpg" width="512px"/>

photo by [Felix Mittermeier](https://www.pexels.com/ja-jp/photo/957912/)

# Setup

spanner-kase depends on [google-cloud-spanner](https://github.com/googleapis/google-cloud-java/tree/master/google-cloud-clients/google-cloud-spanner), so you should set it to dependencies.

```kotlin
implementation("app.ubie.spanner-kase:1.1.3")
implementation("com.google.cloud:google-cloud-spanner:$GOOGLE_CLOUD_SPANNER_VERSION")
```

# Usage on Ktor

## Add migration SQL to resource dir 

First, add the Migration SQL file to a resource directory.
Typical locations are as follows.

```
$PROJECT_DIR/src/main/resources
``` 

Migration SQL file name rule is below.

```kotlin
V[VERSION]__[NAME].sql
```

- `VERSION` : version number. 1 - 9,223,372,036,854,775,807 (26^3 - 1)
- `NAME`: file name.

e.g.

```sh
.
├── build.gradle.kts
├── src
│   └── ...
├── resources
│   └── db
│       └── migration
│           ├── V1__User.sql
│           ├── V2__Todo.sql
│           └── V3__Permission.sql
...
```

Cloud Spanner's DML syntax is [here](https://cloud.google.com/spanner/docs/dml-syntax)

## Initialize Spanner Client

A spanner instance is required.

```kotlin
val options = SpannerOptions.newBuilder().build()
val projectId = options.projectId
val instanceId = InstanceId.of(projectId, YOUR_INSTANCE_ID)
val databaseId = DatabaseId.of(projectId, instanceId.instance, YOUR_DATABSE_ID)
val spanner = options.service

val databaseAdminClient = spanner.databaseAdminClient
val databaseClient = spanner.getDatabaseClient(databaseId)
```

Enter your values ​​for "YOUR_INSTANCE_ID" and "YOUR_DATABSE_ID".

## Instantiate SpannerKase and do migration.

```kotlin
@kotlin.jvm.JvmOverloads
fun Application.module(
    testing: Boolean = false
) {
    // init Spanner Client
    // ... 

    val spannerKaseDatabaseClient = SpannerKaseDatabaseClient(
        instanceId.instance,
        databaseId.database,
        databaseAdminClient,
        databaseClient
    )
    val migrationDataScanner = ClassLoaderMigrationDataScanner(
        environment.classLoader, // io.ktor.application.Application.environment
        "db/migration" // relative path from resources dir
    )
    val configure = SpannerKase.Configure(
        spannerKaseDatabaseClient,
        migrationDataScanner
    ) 
    SpannerKase(configure).migrate()
}
```
