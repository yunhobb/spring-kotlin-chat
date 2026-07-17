plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))

    implementation(project(":chat-core"))
    implementation(project(":chat-api"))
    implementation(project(":ws-gateway"))
    implementation(project(":fanout"))
    implementation(project(":storage-dynamo"))
    implementation(project(":async-sqs"))
    implementation(project(":integration"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.jackson.module.kotlin)

    testImplementation(libs.spring.boot.starter.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
