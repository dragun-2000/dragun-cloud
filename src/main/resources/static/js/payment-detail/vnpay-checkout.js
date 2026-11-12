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
            showPopup('fail', formatPaymentError(xhr));
            setOrderButtonLoading(orderBtn, false, originalButtonText);
        }
    });
}
