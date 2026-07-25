package vn.co.cake.payment.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import vn.co.cake.entity.Order;
import vn.co.cake.helper.EmailService;

@Component
public class PaymentFulfillmentAlertScheduler {

    private final EmailService emailService;
    private final String adminEmail;

    public PaymentFulfillmentAlertScheduler(
            EmailService emailService,
            @Value("${payment.fulfillment.admin-email:haitv@ominext.com}") String adminEmail) {
        this.emailService = emailService;
        this.adminEmail = adminEmail;
    }

    public void scheduleAfterCommit(Order order, String reason) {
        if (order == null) {
            return;
        }
        Runnable send = () -> sendAlert(order, reason);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send.run();
            }
        });
    }

    private void sendAlert(Order order, String reason) {
        String subject = String.format("[DEBASE][VIETQR] Đã nhận tiền nhưng đơn %s cần xử lý", order.getCode());
        String body = String.format(
                "Hệ thống đã nhận thanh toán VietQR nhưng chưa thể hoàn tất đơn hàng.%n%n"
                        + "Mã đơn: %s%nKhách hàng: %s%nSĐT: %s%nEmail: %s%n"
                        + "Số tiền trả trước: %s%nLý do: %s%n%n"
                        + "Admin cần restock sản phẩm, sau đó vào màn Đơn Hàng Lỗi Sync "
                        + "và bấm \"Tạo lại đơn\".",
                order.getCode(), order.getFullName(), order.getPhone(), order.getEmail(),
                order.getPrepaid(), reason);
        emailService.sendPlainTextEmail(adminEmail, subject, body);
    }
}
