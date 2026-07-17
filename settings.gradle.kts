plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "chat-spring-kotlin"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include(
    ":app",
    ":chat-core",
    ":chat-api",
    ":ws-gateway",
    ":fanout",
    ":storage-dynamo",
    ":async-sqs",
    ":integration",
)
