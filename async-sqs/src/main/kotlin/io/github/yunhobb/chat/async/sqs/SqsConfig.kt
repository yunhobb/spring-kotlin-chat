package io.github.yunhobb.chat.async.sqs

import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
@EnableConfigurationProperties(SqsProperties::class)
class SqsConfig {

    @Bean
    fun sqsClient(props: SqsProperties): SqsClient =
        SqsClient.builder()
            .region(Region.of(props.region))
            .apply { props.endpointOverride?.let { endpointOverride(URI.create(it)) } }
            .build()
}

/**
 * AWS 설정 (CLAUDE.md 규칙: @ConfigurationProperties + 생성자 바인딩 + val).
 * endpointOverride는 로컬 테스트(LocalStack)용.
 */
@ConfigurationProperties("aws")
data class SqsProperties(
    val region: String = "ap-northeast-2",
    val endpointOverride: String? = null,
)
