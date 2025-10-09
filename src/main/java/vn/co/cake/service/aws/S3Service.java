package vn.co.cake.service.aws;

import com.amazonaws.AmazonClientException;
import org.springframework.web.multipart.MultipartFile;
import vn.co.cake.controller.external.dto.SendMessageDTO;

import java.io.IOException;

/**
 * @author PhuocVD
 */

public interface S3Service {

    String uploadImageToS3(MultipartFile file) throws AmazonClientException, IOException;

    void sendSms(String phoneNumber, String message);
}
