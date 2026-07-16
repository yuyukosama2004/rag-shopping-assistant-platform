package com.biyesheji.product.service;

import com.biyesheji.exception.BizException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Component
@ConditionalOnProperty(name = "media.storage-type", havingValue = "s3")
class S3MediaObjectStore implements MediaObjectStore {
    private final S3Client client;
    private final String bucket;
    private final String prefix;

    S3MediaObjectStore(@Value("${media.s3.endpoint:}") String endpoint,
                       @Value("${media.s3.region:us-east-1}") String region,
                       @Value("${media.s3.bucket:}") String bucket,
                       @Value("${media.s3.access-key:}") String accessKey,
                       @Value("${media.s3.secret-key:}") String secretKey,
                       @Value("${media.s3.path-style:true}") boolean pathStyle,
                       @Value("${media.s3.prefix:product-media}") String prefix) {
        if (!StringUtils.hasText(bucket)) throw new IllegalStateException("S3 媒体存储必须配置 bucket");
        if (StringUtils.hasText(accessKey) != StringUtils.hasText(secretKey)) {
            throw new IllegalStateException("S3 access key 与 secret key 必须同时配置或同时留空");
        }
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region)).forcePathStyle(pathStyle);
        if (StringUtils.hasText(endpoint)) builder.endpointOverride(URI.create(endpoint));
        if (StringUtils.hasText(accessKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
        this.client = builder.build();
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
    }

    S3MediaObjectStore(S3Client client, String bucket, String prefix) {
        this.client = client;
        this.bucket = bucket;
        this.prefix = normalizePrefix(prefix);
    }

    @Override
    public void put(String filename, byte[] content, String contentType) {
        client.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key(filename)).contentType(contentType).build(),
                RequestBody.fromBytes(content));
    }

    @Override
    public Resource get(String filename) {
        try {
            ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket).key(key(filename)).build());
            return new ByteArrayResource(object.asByteArray());
        } catch (NoSuchKeyException e) {
            throw new BizException(404, "图片不存在");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) throw new BizException(404, "图片不存在");
            throw e;
        }
    }

    @Override
    public void delete(String filename) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key(filename)).build());
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private String key(String filename) {
        return prefix.isEmpty() ? filename : prefix + "/" + filename;
    }

    private static String normalizePrefix(String value) {
        if (!StringUtils.hasText(value)) return "";
        return value.replaceAll("^/+|/+$", "");
    }
}
