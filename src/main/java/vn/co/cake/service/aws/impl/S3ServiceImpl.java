package vn.co.cake.service.aws.impl;

import io.minio.*;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.co.cake.service.aws.S3Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String imageBucket;

    @Value("${minio.url}")
    private String minioUrl;

    public S3ServiceImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Upload ảnh (MultipartFile) lên MinIO (thư mục images/)
     */
    public String uploadImageToS3(MultipartFile file) throws IOException {
        log.info("** Uploading image to MinIO **");
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File convertedFile = this.convertMultiPartToFile(file);
        String url = this.uploadFileToS3("images/" + fileName, convertedFile);
        convertedFile.delete();
        return url;
    }

    /**
     * Upload file thật lên MinIO và trả về link public/presigned
     */
    public String uploadFileToS3(String objectPath, File contentFile) {
        try {
            // Kiểm tra bucket tồn tại
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(imageBucket).build());
            if (!found) {
                log.info("== Create MinIO bucket ==");
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(imageBucket).build());
            }

            // Upload file
            try (InputStream inputStream = new java.io.FileInputStream(contentFile)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(imageBucket)
                                .object(objectPath)
                                .stream(inputStream, contentFile.length(), -1)
                                .contentType("image/jpeg")
                                .build()
                );
            }

            log.info("** Upload {} success! **", objectPath);

            // ✅ Cách 1: Trả link public (nếu bucket cho phép public access)
            String publicUrl = String.format("%s/%s/%s", minioUrl, imageBucket, objectPath);
            return publicUrl;

            // ✅ Cách 2 (nếu bucket private, dùng presigned link)
            /*
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(imageBucket)
                            .object(objectPath)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
            */

        } catch (MinioException e) {
            log.error("** Upload {} failed! error message = {} **", objectPath, e.getMessage());
            throw new RuntimeException("Upload failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("** Upload {} failed! error message = {} **", objectPath, e.getMessage());
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        }
        return convertedFile;
    }
}
