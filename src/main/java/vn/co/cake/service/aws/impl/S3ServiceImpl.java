package vn.co.cake.service.aws.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import lombok.extern.slf4j.Slf4j;
import vn.co.cake.service.aws.S3Service;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    private static final String UPLOAD_DIR = "/var/www/html/external-images/";

    /**
     * Upload file ảnh vào thư mục local và trả về URL hiển thị.
     */
    public String uploadImageToS3(MultipartFile file) throws IOException {
        log.info("** Uploading image to local folder **");

        // Tạo tên file duy nhất
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // Đảm bảo thư mục tồn tại
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        // Lưu file
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("** Uploaded {} successfully! **", fileName);

        return "/external-images/" + fileName;
    }
}
