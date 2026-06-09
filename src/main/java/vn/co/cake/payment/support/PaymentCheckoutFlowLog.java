package vn.co.cake.payment.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Log luồng thanh toán → tạo đơn DB → đồng bộ Pancake POS (format 1., 2., 3., ...).
 */
public final class PaymentCheckoutFlowLog {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentCheckoutFlowLog.class);

    private PaymentCheckoutFlowLog() {
    }

    public static void step(String traceId, int step, String message, Object... args) {
        String body = formatMessage(message, args);
        LOG.info("[CHECKOUT-FLOW][trace={}] {}. {}", nullToDash(traceId), step, body);
    }

    private static String nullToDash(String traceId) {
        return traceId != null && !traceId.isBlank() ? traceId : "-";
    }

    private static String formatMessage(String message, Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        try {
            return String.format(message.replace("{}", "%s"), args);
        } catch (Exception ex) {
            return message + " " + java.util.Arrays.toString(args);
        }
    }
}
