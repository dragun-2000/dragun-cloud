package vn.co.cake.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AWSS3Config {
    @Value("${aws.access-key-id}")
    private String accessKey;

    @Value("${aws.secret-access-key}")
    private String secretKey;

    @Bean
    public AmazonS3 s3Client() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder.standard()
                .withRegion(Regions.AP_SOUTHEAST_1) 
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
    
    @Bean
    public AmazonSimpleEmailService sesClient() {
        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonSimpleEmailServiceClient.builder()
            .withRegion(Regions.AP_SOUTHEAST_1)
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
            .build();
    }

    @Bean
    public AmazonSNS amazonSNS() {
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonSNSClientBuilder.standard()
                .withRegion(Regions.AP_SOUTHEAST_1)
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .build();
    }
}
