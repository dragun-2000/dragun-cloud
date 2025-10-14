package vn.co.cake.service.impl;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

@Service
public class MinioServiceImpl {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public MinioServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public void uploadFile(MultipartFile file) throws Exception {
        boolean exists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucket).build()
        );
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(file.getOriginalFilename())
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
            );
        }
    }

    public InputStream getFile(String filename) throws Exception {
        return minioClient.getObject(
            GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .build()
        );
    }

    public void deleteFile(String filename) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .build()
        );
    }
}
