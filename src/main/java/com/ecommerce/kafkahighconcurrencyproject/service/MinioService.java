package com.ecommerce.kafkahighconcurrencyproject.service;

import co.fareye.config.ConfigProperty;
import co.fareye.constant.AppConstant;
import co.fareye.dto.CopyDto;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class MinioService {

    private MinioClient minioClient;

    private String minioBucket;

    @Autowired
    private ConfigProperty configProperty;

    @PostConstruct
    public void initialiseMinio() {
        minioClient = MinioClient.builder().endpoint(configProperty.getBucketEndpoint()).credentials(configProperty.getBucketAccessKey(), configProperty.getBucketSecretKey()).region(configProperty.getS3BucketRegion())
                .build();
        this.minioBucket = configProperty.getS3BucketName();
    }

    public String generatePresignedUrl(String key, String bucket, String contentType, boolean privateAccess) {
        return generatePresignedUrl(key, bucket, contentType, AppConstant.HTTP_METHOD.PUT);
    }

    public String downloadFile(String key, String bucket) {
        return generatePresignedUrl(key, bucket, null, AppConstant.HTTP_METHOD.GET);
    }


    public String uploadFile(String fileName, File file, String bucket, Map<String, String> metaData) {
        InputStream targetStream;
        bucket = getBucketName(bucket);
        try {
            targetStream = Files.newInputStream(file.toPath());
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).userMetadata(metaData).object(fileName)
                    .stream(targetStream, -1, 10485760).build());
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                 | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                 | IllegalArgumentException | IOException e) {
            log.error("Unable to upload file {}", e.getMessage());
            throw new RuntimeException("Unable to upload file : " + e.getMessage());
        }
        return getUrl(bucket, fileName);
    }

    public String uploadFile(String fileName, byte[] data, String bucket, Map<String, String> metaData) {
        InputStream targetStream;
        bucket = getBucketName(bucket);
        try {
            targetStream = new ByteArrayInputStream(data);
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).userMetadata(metaData).object(fileName)
                    .stream(targetStream, -1, 10485760).build());
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                 | InvalidResponseException | NoSuchAlgorithmException | ServerException | XmlParserException
                 | IllegalArgumentException | IOException e) {
            log.error("Unable to upload file {}", e.getMessage());
            throw new RuntimeException("Unable to upload file : " + e.getMessage());
        }
        return getUrl(bucket, fileName);
    }

    public String getUrl(String s3BucketName, String path) {
        s3BucketName = getBucketName(s3BucketName);
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET)
                    .bucket(s3BucketName).object(path).expiry(AppConstant.ACCESS_DURATION, TimeUnit.MINUTES).build());
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                 | InvalidResponseException | NoSuchAlgorithmException | XmlParserException | ServerException
                 | IllegalArgumentException | IOException e) {
            log.error("Unable to obtain url for the respective path {}", e.getMessage());
            throw new RuntimeException("Unable to obtain url for the respective path : " + e.getMessage());
        }
    }

    public String getBucketName(String bucket) {

        return bucket != null ? bucket : minioBucket;
    }

    private String generatePresignedUrl(String key, String bucket, String contentType, AppConstant.HTTP_METHOD method) {
        bucket = getBucketName(bucket);
        try {
            return minioClient
                    .getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(requestMethod(method.name()))
                            .bucket(bucket).object(key).expiry(AppConstant.ACCESS_DURATION, TimeUnit.MINUTES).build());
        } catch (InvalidKeyException | ErrorResponseException | InsufficientDataException | InternalException
                 | InvalidResponseException | NoSuchAlgorithmException | XmlParserException | ServerException
                 | IllegalArgumentException | IOException e) {
            log.error("Unable to get presigned object url {}", e.getMessage());
            throw new RuntimeException("Unable to get presigned object url : " + e.getMessage());
        }

    }


    public Object copyFile(CopyDto fileInfo) {
        try {
            if (fileInfo.getSourceKey() != null) {
                String destinationKey;
                destinationKey = getDestinationKey(fileInfo);
                CopySource copySource = CopySource.builder().bucket(getBucketName(null))
                        .object(fileInfo.getSourceKey()).build();
                CopyObjectArgs copyObject = CopyObjectArgs.builder()
                        .bucket(getBucketName(null))
                        .object(destinationKey)
                        .source(copySource)
                        .build();
                minioClient.copyObject(copyObject);
                return fileInfo;
            } else {
                throw new RuntimeException("Source File not found");
            }
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
            throw new RuntimeException("Source File cannot be copied");
        }
    }

    private String getDestinationKey(CopyDto fileInfo) {
        String destinationKey;
        if (fileInfo.getDestinationKey() == null) {
            String[] dirArr = fileInfo.getSourceKey().split(AppConstant.PATH_SEPARATOR);
            dirArr[dirArr.length - 1] = UUID.randomUUID().toString();
            destinationKey = String.join(AppConstant.PATH_SEPARATOR, dirArr);
            fileInfo.setDestinationKey(destinationKey);
        } else {
            destinationKey = fileInfo.getDestinationKey();
        }
        return destinationKey;
    }

    private Method requestMethod(String name) {
        switch (AppConstant.HTTP_METHOD.valueOf(name)) {
            case POST:
                return Method.POST;
            case PUT:
                return Method.PUT;
            case DELETE:
                return Method.DELETE;
            default:
                return Method.GET;
        }
    }
}
