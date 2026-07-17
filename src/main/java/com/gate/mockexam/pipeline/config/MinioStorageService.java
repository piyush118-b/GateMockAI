package com.gate.mockexam.pipeline.config;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Service for all MinIO operations: upload, presign URL, delete, existence check.
 *
 * Object key naming convention:
 *   papers/{paperId}/questions/{questionId}/{filename}
 *   papers/{paperId}/questions/{questionId}/options/{optionId}/{filename}
 *   papers/{paperId}/original_paper.pdf
 *   papers/{paperId}/answer_key.pdf
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /**
     * Uploads bytes to MinIO and returns the object key.
     *
     * @param objectKey  full object path inside the bucket
     * @param data       raw bytes
     * @param mimeType   e.g., "image/png", "application/pdf"
     * @return objectKey for storage in the DB
     */
    public String uploadBytes(String objectKey, byte[] data, String mimeType) {
        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(data), data.length, -1)
                            .contentType(mimeType)
                            .build()
            );
            log.debug("Uploaded {} bytes to minio://{}/{}", data.length, bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload object {} to MinIO: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("MinIO upload failed for key: " + objectKey, e);
        }
    }

    /**
     * Uploads a stream to MinIO.
     */
    public String uploadStream(String objectKey, InputStream stream, long size, String mimeType) {
        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(stream, size, -1)
                            .contentType(mimeType)
                            .build()
            );
            log.debug("Uploaded stream to minio://{}/{}", bucket, objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Failed to upload stream {} to MinIO: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("MinIO stream upload failed for key: " + objectKey, e);
        }
    }

    /**
     * Generates a pre-signed GET URL valid for the specified duration.
     */
    public String generatePresignedUrl(String objectKey, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("MinIO presign failed for key: " + objectKey, e);
        }
    }

    /**
     * Downloads object bytes from MinIO.
     */
    public byte[] downloadBytes(String objectKey) {
        try {
            try (InputStream is = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build())) {
                return is.readAllBytes();
            }
        } catch (Exception e) {
            log.error("Failed to download {} from MinIO: {}", objectKey, e.getMessage(), e);
            throw new RuntimeException("MinIO download failed for key: " + objectKey, e);
        }
    }

    /**
     * Deletes an object from MinIO.
     */
    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.debug("Deleted minio://{}/{}", bucket, objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete {} from MinIO: {}", objectKey, e.getMessage());
        }
    }

    /**
     * Checks whether an object exists in MinIO.
     */
    public boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getBucket() {
        return bucket;
    }

    // ─────────────────────────────────────────────────
    // Object key builder helpers
    // ─────────────────────────────────────────────────

    public static String questionAssetKey(String paperId, String questionId, String filename) {
        return "papers/" + paperId + "/questions/" + questionId + "/" + filename;
    }

    public static String optionAssetKey(String paperId, String questionId, String optionId, String filename) {
        return "papers/" + paperId + "/questions/" + questionId + "/options/" + optionId + "/" + filename;
    }

    public static String paperPdfKey(String paperId, String filename) {
        return "papers/" + paperId + "/" + filename;
    }

    // ─────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build()
                );
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to ensure MinIO bucket exists: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO bucket initialization failed", e);
        }
    }
}
