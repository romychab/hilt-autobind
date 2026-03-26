plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvmToolchain(17)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
}

dependencies {
    implementation(projects.libCore)
    implementation(libs.hilt.core)
    implementation(libs.ksp.api)
    implementation(libs.kotlin.poet)
    implementation(libs.kotlin.poet.ksp)
}
