plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}
