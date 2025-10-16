package vn.co.cake.service.aws;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author PhuocVD
 */

public interface S3Service {

    String uploadImageToS3(MultipartFile file) throws IOException;
}
