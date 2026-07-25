$(function () {
    'use strict';

    function escapeHtml(text) {
        if (text == null) {
            return '';
        }
        return String(text)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function displayValue(value) {
        return value != null && String(value).trim() !== '' ? String(value).trim() : 'N/A';
    }

    function statusBadgeClass(status) {
        var text = (status || '').toLowerCase();
        if (text.indexOf('thất bại') !== -1 || text.indexOf('lỗi') !== -1 || text.indexOf('fail') !== -1) {
            return 'order-detail-modal__badge--danger';
        }
        if (text.indexOf('chờ') !== -1 || text.indexOf('xử lý') !== -1 || text.indexOf('pending') !== -1) {
            return 'order-detail-modal__badge--warning';
        }
        return 'order-detail-modal__badge--success';
    }

    function buildDetailRow(label, value, options) {
        options = options || {};
        var cssClass = options.fullWidth ? ' order-detail-modal__row--full' : '';
        return '<div class="order-detail-modal__row' + cssClass + '">' +
            '<dt>' + escapeHtml(label) + '</dt>' +
            '<dd>' + escapeHtml(displayValue(value)) + '</dd>' +
            '</div>';
    }

    function buildDetailRows($card) {
        var orderCode = displayValue($card.data('order-code'));
        var recipient = $card.data('recipient');
        var phone = $card.data('phone');
        var address = $card.data('address');
        var status = displayValue($card.data('status'));
        var date = $card.data('date');
        var amount = displayValue($card.data('amount'));

        return '<div class="order-detail-modal__summary">' +
            '<div class="order-detail-modal__summary-main">' +
            '<span class="order-detail-modal__code-label">Mã đơn</span>' +
            '<span class="order-detail-modal__code">' + escapeHtml(orderCode) + '</span>' +
            '</div>' +
            '<span class="order-detail-modal__badge ' + statusBadgeClass(status) + '">' +
            escapeHtml(status) + '</span>' +
            '</div>' +
            '<div class="order-detail-modal__section">' +
            '<h6 class="order-detail-modal__section-title">Thông tin giao hàng</h6>' +
            '<dl class="order-detail-modal__grid">' +
            buildDetailRow('Tên người nhận', recipient) +
            buildDetailRow('Số điện thoại', phone) +
            buildDetailRow('Địa chỉ nhận hàng', address, { fullWidth: true }) +
            '</dl>' +
            '</div>' +
            '<div class="order-detail-modal__section">' +
            '<h6 class="order-detail-modal__section-title">Thời gian &amp; thanh toán</h6>' +
            '<dl class="order-detail-modal__grid">' +
            buildDetailRow('Thời gian đặt', date) +
            '</dl>' +
            '<div class="order-detail-modal__amount-box">' +
            '<span class="order-detail-modal__amount-label">COD thanh toán</span>' +
            '<span class="order-detail-modal__amount-value">' + escapeHtml(amount) + '</span>' +
            '</div>' +
            '</div>';
    }

    function showLocalDetailModal($card, pancakeUrl) {
        $('#orderDetailModalBody').html(buildDetailRows($card));
        var $pancakeLink = $('#orderDetailModalPancakeLink');
        if (pancakeUrl) {
            $pancakeLink.attr('href', pancakeUrl).removeClass('d-none');
        } else {
            $pancakeLink.addClass('d-none').attr('href', '#');
        }
        $('#orderDetailModal').modal('show');
    }

    $(document).on('click', '.js-pancake-order-detail', function (e) {
        e.preventDefault();
        var $btn = $(this);
        var pancakeUrl = ($btn.attr('data-tracking-link') || '').toString().trim();
        var $card = $btn.closest('.js-order-card');

        if (pancakeUrl) {
            window.open(pancakeUrl, '_blank', 'noopener,noreferrer');
            return;
        }

        showLocalDetailModal($card, null);
    });

    initVietQrPendingPayment();
});

function initVietQrPendingPayment() {
    var orderId = ($('#vietqr-pending-order-id').val() || '').trim();
    if (!orderId) {
        return;
    }

    var pollIntervalMs = 4000;
    var pollTimer = null;
    var paymentPageUrl = '';
    try {
        paymentPageUrl = sessionStorage.getItem('vietqrPaymentUrl_' + orderId) || '';
    } catch (e) { /* ignore */ }
    var sandboxEnabled = $('#vietqr-sandbox-enabled').length > 0;

    $('#vietqr-wait-order-id').text(orderId);
    if (sandboxEnabled) {
        $('#vietqr-sandbox-simulate-btn').removeClass('d-none');
    }

    function parseStatus(data) {
        if (typeof data === 'string') {
            return { status: String(data).trim().toUpperCase() };
        }
        var details = data || {};
        if (details.status) {
            details.status = String(details.status).trim().toUpperCase();
        }
        if (details.orderCreated === true && details.status !== 'EXPIRED') {
            details.status = 'PAID';
        }
        return details;
    }

    function formatAmount(amount) {
        if (amount === null || amount === undefined || amount === '') {
            return '';
        }
        var num = Number(amount);
        if (isNaN(num)) {
            return String(amount);
        }
        return num.toLocaleString('vi-VN');
    }

    function showWaitModal() {
        $('#vietqrPaymentWaitModal').modal({ backdrop: 'static', keyboard: false, show: true });
    }

    function hideWaitModal() {
        $('#vietqrPaymentWaitModal').modal('hide');
    }

    function showSuccessModal(message) {
        hideWaitModal();
        $('#vietqr-success-msg').text(message || 'Thanh toán thành công! Đơn hàng đang được tạo và đồng bộ lên Pancake POS.');
        $('#vietqrPaymentSuccessModal').modal({ backdrop: 'static', keyboard: false, show: true });
    }

    function stopPolling() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function updateWaitUi(details) {
        if (details.amount != null) {
            $('#vietqr-wait-amount').text(formatAmount(details.amount));
        }
        if (details.content) {
            $('#vietqr-wait-content').text(details.content);
        }
        if (details.qrLink) {
            paymentPageUrl = details.qrLink;
        }

        var status = details.status;
        var msg = details.message || '';

        if (status === 'PAID') {
            stopPolling();
            try {
                sessionStorage.removeItem('vietqrPaymentUrl_' + orderId);
            } catch (e) { /* ignore */ }
            if (typeof updateHeaderBagCount === 'function') {
                updateHeaderBagCount(0);
            }
            showSuccessModal(msg || 'Thanh toán thành công! Đơn hàng đã được tạo.');
            return;
        }
        if (status === 'EXPIRED') {
            stopPolling();
            hideWaitModal();
            $('#vietqr-wait-status-msg').removeClass('text-muted').addClass('text-danger')
                .text(msg || 'Phiên thanh toán đã hết hạn. Vui lòng đặt hàng lại.');
            $('#vietqr-wait-status-hint').text('');
            return;
        }
        if (status === 'PAID_ISSUE') {
            stopPolling();
            $('#vietqr-wait-status-msg').removeClass('text-muted text-success').addClass('text-danger')
                .text(msg || 'Đã nhận thanh toán. Đơn hàng đang cần hỗ trợ xử lý tồn kho.');
            $('#vietqr-wait-status-hint').text('Bộ phận chăm sóc khách hàng sẽ liên hệ với bạn.');
            $('#vietqr-sandbox-simulate-btn').addClass('d-none');
            return;
        }

        $('#vietqr-wait-status-msg').removeClass('text-danger text-success').addClass('text-muted')
            .text('Đang chờ xác nhận thanh toán...');
        $('#vietqr-wait-status-hint').text(msg ||
            'Hoàn tất thanh toán trên tab VietQR (pro.vietqr.vn). Trang này sẽ tự cập nhật.');
    }

    function pollStatus() {
        $.ajax({
            url: '/api/payment/status/' + encodeURIComponent(orderId),
            type: 'GET',
            dataType: 'json',
            cache: false,
            xhrFields: { withCredentials: true }
        }).done(function (data) {
            if (!data || typeof data !== 'object') {
                return;
            }
            if (!data.status && data.result && data.message) {
                $('#vietqr-wait-status-msg').removeClass('text-muted').addClass('text-danger').text(data.message);
                return;
            }
            updateWaitUi(parseStatus(data));
        }).fail(function (xhr) {
            var err = 'Không thể kiểm tra trạng thái thanh toán.';
            if (xhr.status === 403) {
                err = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
            } else if (xhr.responseJSON && xhr.responseJSON.message) {
                err = xhr.responseJSON.message;
            }
            $('#vietqr-wait-status-msg').removeClass('text-muted').addClass('text-danger').text(err);
        });
    }

    $('#vietqr-reopen-payment-btn').on('click', function () {
        if (paymentPageUrl) {
            window.open(paymentPageUrl, '_blank', 'noopener,noreferrer');
        } else {
            pollStatus();
        }
    });

    $('#vietqr-sandbox-simulate-btn').on('click', function () {
        var $btn = $(this);
        $btn.prop('disabled', true);
        $.ajax({
            url: '/api/payment/sandbox-simulate/' + encodeURIComponent(orderId),
            type: 'POST',
            dataType: 'json',
            xhrFields: { withCredentials: true }
        }).done(function (data) {
            updateWaitUi(parseStatus(data));
        }).fail(function (xhr) {
            var err = 'Không thể xác nhận thử.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                err = xhr.responseJSON.message;
            }
            $('#vietqr-wait-status-msg').addClass('text-danger').text(err);
            pollStatus();
        }).always(function () {
            $btn.prop('disabled', false);
        });
    });

    $('#vietqr-success-ok-btn').on('click', function () {
        window.location.href = '/order-history';
    });

    showWaitModal();
    pollStatus();
    pollTimer = setInterval(pollStatus, pollIntervalMs);
}
