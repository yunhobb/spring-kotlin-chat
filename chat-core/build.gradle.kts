plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

// chat-core는 프레임워크·AWS SDK 무의존을 유지한다 (DESIGN.md §4 의존 규칙).
// Spring, Jackson, AWS 의존성을 여기 추가하지 말 것.
dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
