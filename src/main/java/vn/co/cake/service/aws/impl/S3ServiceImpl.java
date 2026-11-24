package vn.co.cake.service.aws.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import vn.co.cake.service.aws.S3Service;
import vn.co.cake.service.image.ImageCompressionService;

@Slf4j
@Service
public class S3ServiceImpl implements S3Service {

    private final ImageCompressionService imageCompressionService;

    public S3ServiceImpl(ImageCompressionService imageCompressionService) {
        this.imageCompressionService = imageCompressionService;
    }

    /**
     * Upload file ảnh vào thư mục local và trả về URL hiển thị.
     * File gốc được lưu nguyên vẹn trước; ảnh > ngưỡng được nén nền tại chỗ (giữ tên file, định dạng).
     */
    public String uploadImageToS3(MultipartFile file) throws IOException {
        log.info("** Uploading image to local folder **");

        // Tạo tên file duy nhất
        String fileName = System.currentTimeMillis() + "_" + safeFileName(file.getOriginalFilename());

        // Đảm bảo thư mục tồn tại
        Path uploadDir = imageCompressionService.uploadDir();
        Files.createDirectories(uploadDir);

        // Lưu file
        Path filePath = uploadDir.resolve(fileName);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("** Uploaded {} successfully! **", fileName);

        imageCompressionService.compressAfterUpload(filePath);

        return "/external-images/" + fileName;
    }

    /** Giữ nguyên tên gốc, chỉ bỏ phần thư mục (chặn "../" ghi ra ngoài thư mục upload). */
    static String safeFileName(String originalFilename) {
        String name = StringUtils.defaultString(originalFilename);
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replace(":", "_").trim();
        if (name.isEmpty() || name.equals(".") || name.equals("..")) {
            return "image";
        }
        return name;
    }
}
