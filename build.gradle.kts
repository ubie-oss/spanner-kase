import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.50"
    id("maven")
    id("signing")
    id("org.jetbrains.dokka") version "0.10.0"
}

group = "app.ubie"
version = "1.0.1"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.cloud:google-cloud-spanner:1.44.0")

    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("org.assertj:assertj-core:3.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val dokka by tasks.getting(DokkaTask::class) {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("javadoc")
    from(dokka)
}

val sourcesJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

artifacts.add("archives", dokkaJar)
artifacts.add("archives", sourcesJar)

signing {
    isRequired = System.getenv("CI")?.toBoolean()?.not() ?: true
    sign(configurations.archives.get())
}

val uploadArchives: Upload by tasks
uploadArchives.apply {
    repositories {
        withConvention(MavenRepositoryHandlerConvention::class) {
            mavenDeployer {
                beforeDeployment {
                    signing.signPom(this)
                }

                val username = if(project.hasProperty("sonatypeUsername")) project.properties["sonatypeUsername"] else System.getenv("sonatypeUsername")
                val password = if(project.hasProperty("sonatypePassword")) project.properties["sonatypePassword"] else System.getenv("sonatypePassword")

                withGroovyBuilder {
                    "snapshotRepository"("url" to "https://oss.sonatype.org/content/repositories/snapshots") {
                        "authentication"("userName" to username, "password" to password)
                    }

                    "repository"("url" to "https://oss.sonatype.org/service/local/staging/deploy/maven2") {
                        "authentication"("userName" to username, "password" to password)
                    }
                }

                pom.project {
                    withGroovyBuilder {
                        "name"("spanner-kase")
                        "artifactId"("spanner-kase")
                        "packaging"("jar")
                        "url"("https://github.com/ubie-inc/spanner-kase")
                        "description"("spanner-kase is the library that schema version management tool")
                        "scm" {
                            "connection"("scm:git:git://github.com/ubie-inc/spanner-kase.git")
                            "developerConnection"("scm:git:ssh://git@github.com:ubie-inc/spanner-kase.git")
                            "url"("https://github.com/ubie-inc/spanner-kase")
                        }
                        "licenses" {
                            "license" {
                                "name"("The Apache Software License, Version 2.0")
                                "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                "distribution"("repo")
                            }
                        }

                        "developers" {
                            "developer" {
                                "id"("sys1yagi")
                                "name"("Toshihiro Yagi")
                            }
                        }

                        "issueManagement" {
                            "system"("github")
                            "url"("https://github.com/ubie-inc/spanner-kase/issues")
                        }
                    }
                }
            }
        }
    }
}
