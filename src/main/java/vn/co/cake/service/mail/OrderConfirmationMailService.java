package vn.co.cake.service.mail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.common.DateConst;
import vn.co.cake.common.StringConst;
import vn.co.cake.dto.OrderConfirmationMailModel;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.OrderItem;
import vn.co.cake.entity.Variation;
import vn.co.cake.helper.EmailService;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.utils.BigDecimalUtil;
import vn.co.cake.utils.DateUtil;

@Slf4j
@Service
public class OrderConfirmationMailService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(StringConst.MAIL_REGEX);

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final TemplateEngine mailTemplateEngine;
    private static final String DEFAULT_STORE_URL = "https://debase.vn";

    private final String storeBaseUrl;
    private final boolean enabled;

    public OrderConfirmationMailService(OrderRepository orderRepository,
                                        EmailService emailService,
                                        @Qualifier("mailTemplateEngine") TemplateEngine mailTemplateEngine,
                                        @Value("${order.confirmation.email.store-url:https://debase.vn}") String storeBaseUrl,
                                        @Value("${order.confirmation.email.enabled:true}") boolean enabled) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.mailTemplateEngine = mailTemplateEngine;
        this.storeBaseUrl = normalizeStoreUrl(storeBaseUrl);
        this.enabled = enabled;
    }

    @Async
    @Transactional(readOnly = true)
    public void sendForOrderAsync(Long orderId) {
        sendForOrder(orderId);
    }

    @Transactional(readOnly = true)
    public boolean sendForOrder(Long orderId) {
        if (!enabled) {
            log.debug("Order confirmation email disabled — skip orderId={}", orderId);
            return false;
        }
        if (orderId == null) {
            return false;
        }
        Order order = orderRepository.findWithItemsAndVariationsById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order confirmation email: order not found id={}", orderId);
            return false;
        }

        String recipient = StringUtils.trimToEmpty(order.getEmail());
        if (!isValidEmail(recipient)) {
            log.warn("Order confirmation email skipped — invalid or empty email for order {}", order.getCode());
            return false;
        }

        try {
            OrderConfirmationMailModel model = buildModel(order);
            Context context = new Context(Locale.forLanguageTag("vi"));
            context.setVariable("mail", model);

            String html = mailTemplateEngine.process(StringConst.SEND_MAIL_ORDER_CONFIRMATION_TEMPLATE, context);
            String subject = StringConst.SEND_MAIL_ORDER_CONFIRMATION_SUBJECT_PREFIX + order.getCode();
            String plainText = buildPlainText(model);

            boolean sent = emailService.sendHtmlEmail(recipient, subject, html, plainText);
            if (sent) {
                log.info("Order confirmation email sent to {} for order {}", recipient, order.getCode());
            } else {
                log.warn("Order confirmation email failed for order {} to {}", order.getCode(), recipient);
            }
            return sent;
        } catch (Exception ex) {
            log.error("Order confirmation email error for orderId={}: {}", orderId, ex.getMessage(), ex);
            return false;
        }
    }

    private OrderConfirmationMailModel buildModel(Order order) {
        List<OrderConfirmationMailModel.LineItem> lines = new ArrayList<>();
        if (order.getOrderItems() != null) {
            for (OrderItem item : order.getOrderItems()) {
                BigDecimal unit = item.getFinalPrice() != null ? item.getFinalPrice() : item.getPrice();
                if (unit == null) {
                    unit = BigDecimal.ZERO;
                }
                BigDecimal lineTotal = unit.multiply(BigDecimal.valueOf(item.getQuantity()));
                Variation variation = item.getVariation();
                String name = variation != null && StringUtils.isNotBlank(variation.getName())
                        ? variation.getName()
                        : "Sản phẩm";
                lines.add(OrderConfirmationMailModel.LineItem.builder()
                        .productName(name)
                        .option(StringUtils.trimToNull(item.getOption()))
                        .quantity(item.getQuantity())
                        .unitPriceDisplay(BigDecimalUtil.formatMoney(unit))
                        .lineTotalDisplay(BigDecimalUtil.formatMoney(lineTotal))
                        .build());
            }
        }

        BigDecimal subtotal = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal grand = subtotal.add(shipping);

        PaymentSummary paymentSummary = resolvePaymentSummary(order.getPaymentMethod(), order.getPrepaid(), grand);

        return OrderConfirmationMailModel.builder()
                .customerName(StringUtils.defaultIfBlank(order.getFullName(), "Quý khách"))
                .orderCode(order.getCode())
                .orderDate(order.getCreated() != null
                        ? DateUtil.dateToString(order.getCreated(), DateConst.YYYY_MM_DD_HH_MM_SS_SLASH)
                        : "")
                .paymentMethodLabel(resolvePaymentMethodLabel(order.getPaymentMethod()))
                .shippingAddress(StringUtils.defaultIfBlank(order.getShippingAddress(), "—"))
                .phone(StringUtils.defaultIfBlank(order.getPhone(), "—"))
                .email(order.getEmail())
                .note(StringUtils.trimToNull(order.getNote()))
                .subtotalDisplay(BigDecimalUtil.formatMoney(subtotal))
                .shippingFeeDisplay(BigDecimalUtil.formatMoney(shipping))
                .grandTotalDisplay(BigDecimalUtil.formatMoney(grand))
                .paymentStatusLabel(paymentSummary.statusLabel)
                .paidAmountDisplay(BigDecimalUtil.formatMoney(paymentSummary.paid))
                .remainingAmountDisplay(BigDecimalUtil.formatMoney(paymentSummary.remaining))
                .paymentSummary(paymentSummary.summary)
                .paidOnline(paymentSummary.paidOnline)
                .fullyPaid(paymentSummary.fullyPaid)
                .storeUrl(storeBaseUrl)
                .storeDisplayUrl("debase.vn")
                .orderHistoryUrl(storeBaseUrl + "/order-history")
                .items(lines)
                .build();
    }

    private static String normalizeStoreUrl(String url) {
        String base = StringUtils.isNotBlank(url) ? url.trim() : DEFAULT_STORE_URL;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://" + base;
        }
        return base;
    }

    private static PaymentSummary resolvePaymentSummary(String paymentMethod, BigDecimal prepaid, BigDecimal grandTotal) {
        boolean vietQr = PaymentConstants.METHOD_VIETQR.equalsIgnoreCase(paymentMethod)
                || PaymentConstants.METHOD_TRANSFER_LEGACY.equalsIgnoreCase(paymentMethod);
        boolean cod = PaymentConstants.METHOD_COD.equalsIgnoreCase(paymentMethod);

        BigDecimal safeGrand = grandTotal != null ? grandTotal : BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal remaining = safeGrand;

        if (vietQr) {
            paid = prepaid != null && prepaid.compareTo(BigDecimal.ZERO) > 0 ? prepaid : safeGrand;
            if (paid.compareTo(safeGrand) > 0) {
                paid = safeGrand;
            }
            remaining = safeGrand.subtract(paid);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) {
                remaining = BigDecimal.ZERO;
            }
            boolean fullyPaid = remaining.compareTo(BigDecimal.ZERO) == 0;
            String status = fullyPaid ? "Đã thanh toán" : "Đã thanh toán một phần";
            String summary = fullyPaid
                    ? String.format("Đơn hàng đã được thanh toán đủ %s VND qua VietQR. Cảm ơn bạn!",
                            BigDecimalUtil.formatMoney(paid))
                    : String.format("Đơn hàng đã thanh toán %s VND qua VietQR. Số tiền còn lại: %s VND.",
                            BigDecimalUtil.formatMoney(paid), BigDecimalUtil.formatMoney(remaining));
            return new PaymentSummary(status, paid, remaining, summary, true, fullyPaid);
        }

        if (cod) {
            String summary = String.format(
                    "Đơn hàng chưa thanh toán. Bạn vui lòng thanh toán %s VND khi nhận hàng (COD).",
                    BigDecimalUtil.formatMoney(safeGrand));
            return new PaymentSummary("Chưa thanh toán", BigDecimal.ZERO, safeGrand, summary, false, false);
        }

        BigDecimal fallbackPaid = prepaid != null ? prepaid.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        if (fallbackPaid.compareTo(safeGrand) > 0) {
            fallbackPaid = safeGrand;
        }
        remaining = safeGrand.subtract(fallbackPaid).max(BigDecimal.ZERO);
        boolean fullyPaid = remaining.compareTo(BigDecimal.ZERO) == 0;
        String status = fullyPaid ? "Đã thanh toán" : (fallbackPaid.compareTo(BigDecimal.ZERO) > 0
                ? "Đã thanh toán một phần" : "Chưa thanh toán");
        return new PaymentSummary(status, fallbackPaid, remaining,
                String.format("Tổng đơn %s VND. Đã thanh toán %s VND, còn lại %s VND.",
                        BigDecimalUtil.formatMoney(safeGrand),
                        BigDecimalUtil.formatMoney(fallbackPaid),
                        BigDecimalUtil.formatMoney(remaining)),
                fallbackPaid.compareTo(BigDecimal.ZERO) > 0, fullyPaid);
    }

    private static final class PaymentSummary {
        final String statusLabel;
        final BigDecimal paid;
        final BigDecimal remaining;
        final String summary;
        final boolean paidOnline;
        final boolean fullyPaid;

        PaymentSummary(String statusLabel, BigDecimal paid, BigDecimal remaining, String summary,
                       boolean paidOnline, boolean fullyPaid) {
            this.statusLabel = statusLabel;
            this.paid = paid;
            this.remaining = remaining;
            this.summary = summary;
            this.paidOnline = paidOnline;
            this.fullyPaid = fullyPaid;
        }
    }

    private static String resolvePaymentMethodLabel(String method) {
        if (PaymentConstants.METHOD_VIETQR.equalsIgnoreCase(method)
                || PaymentConstants.METHOD_TRANSFER_LEGACY.equalsIgnoreCase(method)) {
            return "Chuyển khoản VietQR";
        }
        if (PaymentConstants.METHOD_COD.equalsIgnoreCase(method)) {
            return "Thanh toán khi nhận hàng (COD)";
        }
        return StringUtils.defaultIfBlank(method, "—");
    }

    private static boolean isValidEmail(String email) {
        return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    private static String buildPlainText(OrderConfirmationMailModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("Xin chào ").append(model.getCustomerName()).append(",\n\n");
        sb.append("Cảm ơn bạn đã đặt hàng tại DEBASE.VN.\n");
        sb.append("Mã đơn: ").append(model.getOrderCode()).append("\n");
        sb.append("Ngày đặt: ").append(model.getOrderDate()).append("\n");
        sb.append("Thanh toán: ").append(model.getPaymentMethodLabel()).append("\n\n");
        sb.append("--- Thông tin khách hàng ---\n");
        sb.append("Họ tên: ").append(model.getCustomerName()).append("\n");
        sb.append("Số điện thoại: ").append(StringUtils.defaultIfBlank(model.getPhone(), "—")).append("\n");
        sb.append("Địa chỉ nhận hàng: ").append(StringUtils.defaultIfBlank(model.getShippingAddress(), "—")).append("\n");
        if (StringUtils.isNotBlank(model.getEmail())) {
            sb.append("Email: ").append(model.getEmail()).append("\n");
        }
        if (StringUtils.isNotBlank(model.getNote())) {
            sb.append("Ghi chú: ").append(model.getNote()).append("\n");
        }
        sb.append("\nSản phẩm:\n");
        for (OrderConfirmationMailModel.LineItem line : model.getItems()) {
            sb.append("- ").append(line.getProductName());
            if (line.getOption() != null) {
                sb.append(" (").append(line.getOption()).append(")");
            }
            sb.append(" x").append(line.getQuantity());
            sb.append(" — ").append(line.getLineTotalDisplay()).append(" VND\n");
        }
        sb.append("\nTạm tính: ").append(model.getSubtotalDisplay()).append(" VND\n");
        sb.append("Phí vận chuyển: ").append(model.getShippingFeeDisplay()).append(" VND\n");
        sb.append("Tổng cộng: ").append(model.getGrandTotalDisplay()).append(" VND\n");
        sb.append("Trạng thái: ").append(model.getPaymentStatusLabel()).append("\n");
        sb.append("Đã thanh toán: ").append(model.getPaidAmountDisplay()).append(" VND\n");
        sb.append("Còn lại: ").append(model.getRemainingAmountDisplay()).append(" VND\n");
        sb.append(model.getPaymentSummary()).append("\n\n");
        sb.append("Xem đơn: ").append(model.getOrderHistoryUrl()).append("\n");
        sb.append("DEBASE.VN — ").append(model.getStoreUrl()).append("\n");
        return sb.toString();
    }
}
