package io.github.yunhobb.chat.async.sqs

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient

@Configuration
class SqsConfig {

    @Bean
    fun sqsClient(@Value("\${aws.region:ap-northeast-2}") region: String): SqsClient =
        SqsClient.builder()
            .region(Region.of(region))
            .build()
}
