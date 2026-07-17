plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

// 의존 규칙: chat-core의 port만 바라본다. 다른 어댑터 모듈(fanout, storage-*)을 직접 참조하면
// 향후 Netty/WebFlux 게이트웨이로 추출할 수 없다 (DESIGN.md §4 분리 우선순위 1번).
dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(project(":chat-core"))
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.web)
    implementation(libs.spring.context)
}
