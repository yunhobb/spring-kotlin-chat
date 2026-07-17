plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(platform(libs.aws.bom))
    implementation(project(":chat-core"))
    implementation(libs.spring.context)
    implementation(libs.aws.sqs)
}
