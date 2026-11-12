/**
 * Thanh toán VNPAY: tạo đơn qua /api/vnpay/create rồi chuyển trình duyệt sang cổng VNPAY.
 * Dùng các hàm chung của payment-detail.js: buildPaymentHeaders, formatPaymentError, showPopup,
 * setOrderButtonLoading.
 */
function startVnpayCheckout(data, orderBtn, originalButtonText) {
    $.ajax({
        type: 'POST',
        contentType: 'application/json',
        url: '/api/vnpay/create',
        data: JSON.stringify(data),
        headers: buildPaymentHeaders(),
        success: function (response) {
            if (response && response.paymentUrl) {
                window.location.href = response.paymentUrl;
                return;
            }
            showPopup('fail', 'Không nhận được đường dẫn thanh toán VNPAY. Vui lòng thử lại.');
            setOrderButtonLoading(orderBtn, false, originalButtonText);
        },
        error: function (xhr) {
            console.error('vnpay/create failed', xhr.status, xhr.responseJSON || xhr.responseText);
            var errMsg = formatPaymentError(xhr);
            if (typeof shouldShowVnpayPilotPopup === 'function' && shouldShowVnpayPilotPopup()) {
                setOrderButtonLoading(orderBtn, false, originalButtonText);
                showVnpayPilotPopup(orderBtn, originalButtonText);
                return;
            }
            if (errMsg && errMsg.indexOf('pilot') !== -1 && typeof showVnpayPilotPopup === 'function') {
                setOrderButtonLoading(orderBtn, false, originalButtonText);
                showVnpayPilotPopup(orderBtn, originalButtonText);
                return;
            }
            showPopup('fail', errMsg);
            setOrderButtonLoading(orderBtn, false, originalButtonText);
        }
    });
}
