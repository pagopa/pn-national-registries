package it.pagopa.pn.national.registries.config;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;

@Configuration
public class SqsConfiguration {

    private final String awsRegion;

    public SqsConfiguration(@Value("${aws.region}") String awsRegion) {
        this.awsRegion = awsRegion;
    }

    @Bean
    public AmazonSQS sqsClient() {
        return AmazonSQSClient.builder()
                .withEndpointConfiguration(new AwsClientBuilder
                        .EndpointConfiguration("https://sqs.eu-south-1.amazonaws.com/558518206506/pn-inipec",awsRegion))
                .withRegion(String.valueOf(Region.of(awsRegion)))
                .build();
    }
}
