package io.github.yunhobb.chat.storage.dynamo

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

/**
 * 동기 클라이언트를 쓴다 — 요청 처리는 가상 스레드 위에서 블로킹해도 된다 (DESIGN.md §1-3).
 * 자격 증명은 EKS의 IRSA(default credentials chain)로 해석된다.
 */
@Configuration
class DynamoDbConfig {

    @Bean
    fun dynamoDbClient(@Value("\${aws.region:ap-northeast-2}") region: String): DynamoDbClient =
        DynamoDbClient.builder()
            .region(Region.of(region))
            .build()

    @Bean
    fun dynamoDbEnhancedClient(client: DynamoDbClient): DynamoDbEnhancedClient =
        DynamoDbEnhancedClient.builder()
            .dynamoDbClient(client)
            .build()
}
