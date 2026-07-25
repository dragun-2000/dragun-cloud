package vn.co.cake.payment.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import vn.co.cake.constants.OrderConstants;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Voucher;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.service.OrderService;

@Component
public class PaymentOrderTotalCalculator {

    private final OrderService orderService;
    private final VoucherRepository voucherRepository;

    public PaymentOrderTotalCalculator(OrderService orderService, VoucherRepository voucherRepository) {
        this.orderService = orderService;
        this.voucherRepository = voucherRepository;
    }

    public long calculateGrandTotalVnd(List<OrderItem> orderItems, String voucherCode) {
        BigDecimal subtotal = orderService.calculateItemsSubtotal(orderItems);
        long discount = resolveVoucherDiscountVnd(subtotal, voucherCode);
        int shippingFee = resolveShippingFeeVnd(subtotal.longValue());
        return subtotal.longValue() - discount + shippingFee;
    }

    public boolean isCodAllowed(List<OrderItem> orderItems, String voucherCode) {
        return calculateGrandTotalVnd(orderItems, voucherCode) <= PaymentConstants.COD_MAX_ORDER_TOTAL_VND;
    }

    private long resolveVoucherDiscountVnd(BigDecimal subtotal, String voucherCode) {
        if (!StringUtils.hasText(voucherCode)) {
            return 0L;
        }
        Voucher voucher = voucherRepository.findFirstByCodeAndDeletedIsFalse(voucherCode.trim());
        if (voucher == null || voucher.getDiscountPercent() == null || voucher.getDiscountPercent() <= 0) {
            return 0L;
        }
        return subtotal.multiply(BigDecimal.valueOf(voucher.getDiscountPercent()))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR)
                .longValue();
    }

    private int resolveShippingFeeVnd(long subtotalVnd) {
        Voucher shippingFeeVoucher = voucherRepository.findFirstByCodeAndDeletedIsFalse(
                OrderConstants.VOUCHER_SHIPPING_FEE);
        if (shippingFeeVoucher != null && subtotalVnd < OrderConstants.FREE_SHIPPING_THRESHOLD) {
            return shippingFeeVoucher.getShippingFee();
        }
        return 0;
    }
}
