package io.github.yunhobb.chat.storage.dynamo

import java.net.URI
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/**
 * 동기 클라이언트를 쓴다 — 요청 처리는 가상 스레드 위에서 블로킹해도 된다 (docs/architecture.md §1-3).
 * 자격 증명은 EKS의 IRSA(default credentials chain)로 해석된다.
 */
@Configuration
@EnableConfigurationProperties(AwsProperties::class)
class DynamoDbConfig {

    @Bean
    fun dynamoDbClient(props: AwsProperties): DynamoDbClient =
        DynamoDbClient.builder()
            .region(Region.of(props.region))
            .apply { props.endpointOverride?.let { endpointOverride(URI.create(it)) } }
            .build()

    @Bean
    fun dynamoDbEnhancedClient(client: DynamoDbClient): DynamoDbEnhancedClient =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
}

/**
 * AWS 설정 (CLAUDE.md 규칙: 설정은 @ConfigurationProperties + 생성자 바인딩 + val).
 * region 기본값은 EKS 리전. endpointOverride는 로컬 테스트(DynamoDB Local/LocalStack)용이며
 * 운영에선 미설정으로 두면 AWS 실제 엔드포인트를 쓴다.
 */
@ConfigurationProperties("aws")
data class AwsProperties(
    val region: String = "ap-northeast-2",
    val endpointOverride: String? = null,
)
