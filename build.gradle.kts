import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.render.TextReportRenderer
import com.github.jk1.license.filter.LicenseBundleNormalizer

buildscript {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("io.github.gradle-nexus:publish-plugin:2.0.0")
    }
}

plugins {
    kotlin("jvm") version "2.0.10"
    `maven-publish`
    id("com.github.jk1.dependency-license-report") version "2.9"
    signing
}

if (rootProject.name == "networking") {
    apply {
        plugin("io.github.gradle-nexus.publish-plugin")
    }
    
    configure<io.github.gradlenexus.publishplugin.NexusPublishExtension> {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
                snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
            }
        }
    }
}

licenseReport {
    renderers = arrayOf(
        TextReportRenderer()
    )
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

repositories {
    mavenCentral()
}

// Define group and version based on root project or use defaults for standalone
val projectGroup = "software.bevel"
val projectVersion = "1.0.0"

group = projectGroup
version = projectVersion

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

    implementation("io.projectreactor:reactor-core:3.7.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.2")
    implementation("org.slf4j:slf4j-api:2.0.17")

    // Handle dependencies differently based on whether we're in standalone or multi-project mode
    if (rootProject.name == "networking") {
        // In standalone mode, use the published Maven artifact
        implementation("$projectGroup:file-system-domain:$projectVersion")
    } else {
        // In multi-project mode, use the project dependency
        implementation(project(":file-system-domain"))
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            groupId = projectGroup
            artifactId = "networking"
            version = projectVersion
            
            pom {
                name.set("Networking")
                description.set("Networking library for Bevel")
                url.set("https://bevel.software")
                
                licenses {
                    license {
                        name.set("Mozilla Public License 2.0")
                        url.set("https://www.mozilla.org/en-US/MPL/2.0/")
                    }
                }

                developers {
                    developer {
                        name.set("Razvan-Ion Radulescu")
                        email.set("razvan.radulescu@bevel.software")
                        organization.set("Bevel Software")
                        organizationUrl.set("https://bevel.software")
                    }
                }

                scm {
                    connection.set("scm:git:https://github.com/Bevel-Software/networking.git")
                    developerConnection.set("scm:git:ssh://git@github.com:Bevel-Software/networking.git")
                    url.set("https://github.com/Bevel-Software/networking")
                }
            }
        }
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

// Configure signing
signing {
    sign(publishing.publications["maven"])
}