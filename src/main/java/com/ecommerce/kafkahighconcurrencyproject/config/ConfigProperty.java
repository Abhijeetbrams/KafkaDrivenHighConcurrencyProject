
package com.ecommerce.kafkahighconcurrencyproject.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;


@Configuration
@RefreshScope
@Data
public class ConfigProperty {

    @Value("${fareye.auth.default.user:}")
    private String defaultUser;

    @Value("${fareye.auth.default.password:}")
    private String defaultPassword;

    @Value("${fareye.auth.default.authority:}")
    private String[] defaultAuthority;

    @Value("${fareye.auth.whitelist.url}")
    private String[] whitelistUrl;

//    @Value("${spring.datasource.username}")
//    private String dbUser;
//
//    @Value("${spring.datasource.password}")
//    private String dbPassword;
//
//    @Value("jdbc:postgresql://${spring.datasource.url}/")
//    private String dbUrl;

//    @Value("${app.dbName}")
//    private String dbName;
//
//    @Value("${app.schemaName}")
//    private String dbSchema;

    @Value("${fareye.auth-email-topic}")
    private String emailTopic;

    @Value("${fareye.traffic-log-topic}")
    private String trafficLogTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServerAddress;

    @Value("${spring.rabbitmq.host}")
    private String rabbitHost;

    @Value("${spring.rabbitmq.virtual-host}")
    private String rabbitVirtualHost;

    @Value("${spring.rabbitmq.username}")
    private String rabbitUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitPassword;

    @Value("${fareye.app-queue:app-queue}")
    private String appQueue;

    @Value("${fareye.app-exchange:app-exchange}")
    private String appExchange;

    @Value("${fareye.app-routing-key:app-routing-key}")
    private String appRoutingKey;

    @Value("${s3.accessKey:}")
    private String s3AccessKey;

    @Value("${s3.secret-key:}")
    private String s3SecretKey;

    @Value("${s3.bucket-name:}")
    private String s3BucketName;

    @Value("${s3.bucket-region}")
    private String s3BucketRegion;

    @Value("${app.alert.emails:baldeep.kwatra@getfareye.com}")
    private String alertEmails;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.cloud.config.profile}")
    private String environment;

    @Value("${fareye.inbound-request-topic}")
    private String inboundRequestTopic;

    @Value("${opentracing.jaeger.udp-sender.port:6831}")
    private int jaegerUdpPort;

    @Value("${opentracing.jaeger.udp-sender.host:localhost}")
    private String jaegerUdpHost;

    @Value("${opentracing.jaeger.log-spans:false}")
    private boolean jaegerLogSpans;

    @Value("${encrypt-key}")
    private String encryptKey;

    @Value("${app.kafka.max-block.seconds:60}")
    private int kafkaMaxBlockSeconds;

    @Value("${app-master.ratelimiter-url:}")
    private String ratelimiterURL;

    @Value("${s3.aws.bucket-endpoint:}")
    private String awsBucketEndPoint;

    @Value("${fareye.connector-topic}")
    private String asyncTopic;

    @Value("${kafka.ekart-rate-topic}")
    private String ekartRateTopic;

    @Value("${kafka.ekart-rate-send-topic}")
    private String ekartRateSendTopic;

    @Value("${consumer.ekart-consumer-status}")
    private String ekartConsumerStatus;

    @Value("${consumer.ekart-consumer-concurrency}")
    private String ekartConsumerConcurrency;

    @Value("${kafka.ekart-raw-data-topic}")
    private String ekartRawDataTopic;

    @Value("${consumer.ekart-raw-data-consumer-status}")
    private String ekartRawDataConsumerStatus;

    @Value("${consumer.ekart-raw-data-consumer-concurrency}")
    private String ekartRawDataConsumerConcurrency;

    @Value("${consumer.ekart-async-service-code}")
    private String ekartAsyncServiceCode;

    @Value("${consumer.ekart-gateway-path}")
    private String ekartGatewayPath;

    @Value("${consumer.ekart-async-master-code}")
    private String ekartAsyncMasterCode;

    @Value("${consumer.ekart-schema-name}")
    private String ekartSchemaName;

    @Value("${app.user-id}")
    private String userId;

    @Value("${app.user-email}")
    private String userEmail;

    @Value("${app.organization-id}")
    private String organizationId;

    @Value("${app.access-token}")
    private String accessToken;

    @Value("${app.ekart-company-id}")
    private String ekartCompanyId;

    @Value("${ekart.datasource.username}")
    private String ekartDBUsername;

    @Value("${ekart.datasource.password}")
    private String ekartDBPassword;

    @Value("jdbc:postgresql://${ekart.datasource.url}/${ekart.datasource.name}")
    private String ekartDBUrl;

    @Value("${ekart.datasource.name}")
    private String ekartDBName;

    @Value("${ekart.datasource.schema}")
    private String ekartDBSchemaName;

    @Value("${sftp.host}")
    private String sftpHost;

    @Value("${sftp.port}")
    private int sftpPort;

    @Value("${sftp.username}")
    private String sftpUsername;

    @Value("${sftp.password}")
    private String sftpPassword;

    @Value("${sftp.upload-directory}")
    private String uploadDirectory;

    @Value("${sftp.upload-failed-directory}")
    private String uploadFailedDirectory;

    @Value("${sftp.upload-batch-size}")
    private int sftpUploadBatchSize;

    @Value("${s3.bucket.endpoint}")
    private String bucketEndpoint;

    @Value("${s3.bucket.accessKey}")
    private String bucketAccessKey;

    @Value("${s3.bucket-key}")
    private String bucketSecretKey;


}
