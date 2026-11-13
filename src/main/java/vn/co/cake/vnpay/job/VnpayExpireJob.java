package vn.co.cake.vnpay.job;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.repository.VnpayPaymentRepository;
import vn.co.cake.vnpay.service.VnpayExpireService;

/**
 * Mỗi phút (:30): phiên VNPAY PENDING đã quá {@code expiresAt + expireGraceMinutes}
 * → QueryDR → PAID / EXPIRED; nếu QueryDR lỗi thì huỷ khi {@code now >= expiresAt + queryDrGiveUpMinutes}.
 * Độc lập với Job004 (VietQR).
 */
@Slf4j
@Component
public class VnpayExpireJob {

    private final VnpayProperties properties;
    private final VnpayPaymentRepository vnpayPaymentRepository;
    private final VnpayExpireService vnpayExpireService;

    public VnpayExpireJob(VnpayProperties properties,
                          VnpayPaymentRepository vnpayPaymentRepository,
                          VnpayExpireService vnpayExpireService) {
        this.properties = properties;
        this.vnpayPaymentRepository = vnpayPaymentRepository;
        this.vnpayExpireService = vnpayExpireService;
    }

    @Scheduled(cron = "30 * * * * ?")
    public void reconcileExpiredPayments() {
        if (!properties.isConfigured()) {
            return;
        }
        Date deadline = new Date(System.currentTimeMillis()
                - TimeUnit.MINUTES.toMillis(properties.getExpireGraceMinutes()));
        List<String> txnRefs = vnpayPaymentRepository.findPendingTxnRefsExpiredBefore(deadline);
        for (String txnRef : txnRefs) {
            try {
                vnpayExpireService.reconcile(txnRef);
            } catch (Exception ex) {
                log.error("VnpayExpireJob: reconcile failed txnRef={}", txnRef, ex);
            }
        }
        if (!txnRefs.isEmpty()) {
            log.info("VnpayExpireJob: checked {} expired VNPAY session(s)", txnRefs.size());
        }
    }
}
