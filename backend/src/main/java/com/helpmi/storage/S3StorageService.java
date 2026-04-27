package com.helpmi.storage;

import com.helpmi.exception.NotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @PostConstruct
    void ensureBucket() {
        try {
            s3Client.headBucket(r -> r.bucket(bucket));
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(r -> r.bucket(bucket));
        }
    }

    @Override
    public void store(String key, InputStream content, long contentLength, String contentType) throws IOException {
        s3Client.putObject(
                r -> r.bucket(bucket).key(key).contentType(contentType).contentLength(contentLength),
                RequestBody.fromInputStream(content, contentLength));
    }

    @Override
    public InputStream retrieve(String key) throws IOException {
        try {
            return s3Client.getObject(r -> r.bucket(bucket).key(key));
        } catch (NoSuchKeyException e) {
            throw new NotFoundException("Fichier introuvable");
        }
    }

    @Override
    public void delete(String key) throws IOException {
        s3Client.deleteObject(r -> r.bucket(bucket).key(key));
    }
}
