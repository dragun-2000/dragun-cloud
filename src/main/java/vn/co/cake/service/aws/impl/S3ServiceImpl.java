package vn.co.cake.service.aws.impl;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.co.cake.common.DateConst;
//import vn.co.cake.helper.EmailService;
import vn.co.cake.service.aws.S3Service;
import vn.co.cake.utils.DateUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author HaiTV
 */
@Service
@Slf4j
public class S3ServiceImpl implements S3Service {
    private final AmazonS3 s3client;

    private final AmazonSNS amazonSNS;
    
    @Value("${aws.s3.image}")
    private String imageBucket;

    public S3ServiceImpl(AmazonS3 s3client, AmazonSNS amazonSNS) {
        this.s3client = s3client;
        this.amazonSNS = amazonSNS;
    }

    @Override
    public String uploadImageToS3(MultipartFile file) throws AmazonClientException, IOException {
        log.info("** Uploading image **");
        String fileName = String.format("%s_%s", DateUtil.dateToString(DateUtil.now(), DateConst.YYYYMMDDHHMMSS), file.getOriginalFilename());
        File convertedFile = this.convertMultiPartToFile(file);
        return this.uploadFileToS3(fileName, convertedFile);
    }
    
    public String uploadFileToS3(String fileName, File contentFile) throws AmazonClientException {
        log.info("** Uploading medis file **");
        upload(imageBucket, fileName, contentFile);
        contentFile.delete();
        
        return s3client.getUrl(imageBucket, fileName).toString();
    }

    private File convertMultiPartToFile(MultipartFile file) throws IOException {
        File convertedFile = new File(file.getOriginalFilename());
        try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
            fos.write(file.getBytes());
        }
        return convertedFile;
    }
    
    private void upload(String bucket, String fileName, File contentFile) throws AmazonClientException {
        try {
            // Put Object
            if (!s3client.doesBucketExistV2(bucket)) {
                log.info("== Create s3 bucket ==");
                s3client.createBucket(new CreateBucketRequest(bucket));
            }

            s3client.putObject(new PutObjectRequest(imageBucket, fileName, contentFile));

            log.info("** Upload {} success! **", fileName);
        } catch (Exception e) {
            log.info("** Upload {} failed! error message = {} **", fileName, e.getMessage());
        }
    }

    public void sendSms(String phoneNumber, String message) {
        PublishRequest request = new PublishRequest()
                .withMessage(message)
                .withPhoneNumber(phoneNumber); // Ví dụ: "+84987654321"
        amazonSNS.publish(request);
    }
}
