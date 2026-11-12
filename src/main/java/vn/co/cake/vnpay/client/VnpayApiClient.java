package vn.co.cake.vnpay.client;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpayQueryDrResult;
import vn.co.cake.vnpay.entity.VnpayPayment;
import vn.co.cake.vnpay.support.VnpayFormats;
import vn.co.cake.vnpay.support.VnpaySigner;

/**
 * Tạo URL thanh toán (redirect) và gọi API truy vấn giao dịch (querydr) của VNPAY.
 */
@Slf4j
@Component
public class VnpayApiClient {

    private final VnpayProperties properties;
    private final RestTemplate restTemplate;

    public VnpayApiClient(VnpayProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    public String buildPaymentUrl(VnpayPayment payment, Date createdAt, String locale) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", properties.getVersion());
        params.put("vnp_Command", VnpayConstants.COMMAND_PAY);
        params.put("vnp_TmnCode", properties.getTmnCode());
        params.put("vnp_Amount", VnpayFormats.toVnpAmount(payment.getAmount()));
        params.put("vnp_CurrCode", VnpayConstants.CURR_CODE_VND);
        params.put("vnp_TxnRef", payment.getTxnRef());
        params.put("vnp_OrderInfo", payment.getOrderInfo());
        params.put("vnp_OrderType", properties.getOrderType());
        params.put("vnp_Locale", locale);
        params.put("vnp_ReturnUrl", properties.getReturnUrl());
        params.put("vnp_IpAddr", payment.getClientIp());
        params.put("vnp_CreateDate", VnpayFormats.formatDate(createdAt));
        params.put("vnp_ExpireDate", VnpayFormats.formatDate(payment.getExpiresAt()));

        String query = VnpaySigner.buildQuery(params);
        String secureHash = VnpaySigner.hmacSha512(properties.getHashSecret(), query);
        return properties.getPayUrl() + "?" + query + "&" + VnpayConstants.PARAM_SECURE_HASH + "=" + secureHash;
    }

    /**
     * QueryDR — hỏi VNPAY kết quả giao dịch khi không nhận được IPN.
     * Checksum: RequestId|Version|Command|TmnCode|TxnRef|TransactionDate|CreateDate|IpAddr|OrderInfo
     */
    public VnpayQueryDrResult queryDr(VnpayPayment payment, String serverIp) {
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String createDate = VnpayFormats.formatDate(new Date());
        String orderInfo = VnpayFormats.toOrderInfo("Truy van giao dich " + payment.getTxnRef());

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", properties.getVersion());
        body.put("vnp_Command", VnpayConstants.COMMAND_QUERYDR);
        body.put("vnp_TmnCode", properties.getTmnCode());
        body.put("vnp_TxnRef", payment.getTxnRef());
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", payment.getVnpCreateDate());
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", serverIp);
        body.put("vnp_SecureHash", VnpaySigner.signPipe(properties.getHashSecret(),
                requestId, properties.getVersion(), VnpayConstants.COMMAND_QUERYDR, properties.getTmnCode(),
                payment.getTxnRef(), payment.getVnpCreateDate(), createDate, serverIp, orderInfo));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> response = restTemplate.exchange(properties.getApiUrl(), HttpMethod.POST,
                new HttpEntity<>(body, headers), new ParameterizedTypeReference<Map<String, Object>>() {
                }).getBody();

        VnpayQueryDrResult result = VnpayQueryDrResult.from(response);
        result.setSignatureValid(verifyQueryDrResponse(result));
        log.info("VNPAY querydr txnRef={} responseCode={} transactionStatus={} signatureValid={}",
                payment.getTxnRef(), result.getResponseCode(), result.getTransactionStatus(),
                result.isSignatureValid());
        return result;
    }

    /**
     * Checksum response theo tài liệu: ResponseId|Command|ResponseCode|Message|TmnCode|TxnRef|Amount|BankCode|
     * PayDate|TransactionNo|TransactionType|TransactionStatus|OrderInfo|PromotionCode|PromotionAmount.
     * Tài liệu VNPAY thiếu một dấu '|' giữa Amount và BankCode — cần VNPAY xác nhận công thức.
     */
    private boolean verifyQueryDrResponse(VnpayQueryDrResult result) {
        Map<String, String> raw = result.getRaw();
        String expected = VnpaySigner.signPipe(properties.getHashSecret(),
                raw.get("vnp_ResponseId"), raw.get("vnp_Command"), raw.get("vnp_ResponseCode"),
                raw.get("vnp_Message"), raw.get("vnp_TmnCode"), raw.get("vnp_TxnRef"), raw.get("vnp_Amount"),
                raw.get("vnp_BankCode"), raw.get("vnp_PayDate"), raw.get("vnp_TransactionNo"),
                raw.get("vnp_TransactionType"), raw.get("vnp_TransactionStatus"), raw.get("vnp_OrderInfo"),
                raw.get("vnp_PromotionCode"), raw.get("vnp_PromotionAmount"));
        return expected.equalsIgnoreCase(raw.getOrDefault(VnpayConstants.PARAM_SECURE_HASH, ""));
    }
}
