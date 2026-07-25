$(function () {
    const orderId = $('#vietqr-order-id').text().trim();
    if (!orderId) {
        return;
    }

    const pollIntervalMs = 4000;
    let pollTimer = null;
    const csrfToken = $("input[name='_csrf']").val() || null;

    function parseStatus(data) {
        if (typeof data === 'string') {
            return { status: data };
        }
        return data || {};
    }

    function formatAmount(amount) {
        if (amount === null || amount === undefined || amount === '') {
            return '';
        }
        const num = Number(amount);
        if (isNaN(num)) {
            return String(amount);
        }
        return num.toLocaleString('vi-VN');
    }

    function renderCheckoutDetails(details) {
        if (details.amount != null) {
            $('#vietqr-amount').text(formatAmount(details.amount));
        }
        if (details.content) {
            $('#vietqr-content').text(details.content);
        }

        const qrCode = details.qrCode;
        const qrLink = details.qrLink;
        if (qrCode) {
            const src = qrCode.indexOf('data:') === 0 ? qrCode : 'data:image/png;base64,' + qrCode;
            $('#vietqr-image').attr('src', src);
            $('#qr-container').show();
        } else if (qrLink) {
            $('#qr-container').show();
            $('#vietqr-image').hide();
        }
        if (qrLink) {
            $('#qr-link').attr('href', qrLink).show();
        } else {
            $('#qr-link').hide();
        }

        updateStatusMessage(details);
    }

    function updateStatusMessage(details) {
        const status = details.status;
        const msg = details.message || '';

        if (status === 'PAID') {
            $('#payment-status-msg').removeClass('text-muted text-danger').addClass('text-success')
                .text(msg || 'Thanh toán thành công! Đang chuyển đến lịch sử đơn hàng...');
            $('#payment-status-hint').text('');
            $('#sandbox-simulate-btn').hide();
            stopPolling();
            setTimeout(function () {
                window.location.href = '/order-history';
            }, 2000);
            return;
        }
        if (status === 'EXPIRED') {
            $('#payment-status-msg').removeClass('text-muted').addClass('text-danger')
                .text(msg || 'Phiên thanh toán đã hết hạn. Vui lòng đặt hàng lại.');
            $('#payment-status-hint').text('');
            $('#sandbox-simulate-btn').hide();
            stopPolling();
            return;
        }
        if (status === 'PAID_ISSUE') {
            $('#payment-status-msg').removeClass('text-muted text-success').addClass('text-danger')
                .text(msg || 'Đã nhận thanh toán. Đơn hàng đang cần hỗ trợ xử lý tồn kho.');
            $('#payment-status-hint').text('Bộ phận chăm sóc khách hàng sẽ liên hệ với bạn.');
            $('#sandbox-simulate-btn').hide();
            stopPolling();
            return;
        }

        $('#payment-status-msg').removeClass('text-danger text-success').addClass('text-muted')
            .text('Đang chờ xác nhận thanh toán...');
        $('#payment-status-hint').text(msg || 'Sau khi chuyển khoản, trang sẽ tự cập nhật trong vài phút.');
        if ($('#sandbox-simulate-btn').length) {
            $('#sandbox-simulate-btn').show();
        }
    }

    function stopPolling() {
        if (pollTimer) {
            clearInterval(pollTimer);
            pollTimer = null;
        }
    }

    function pollStatus() {
        $.ajax({
            url: '/api/payment/status/' + encodeURIComponent(orderId),
            type: 'GET',
            dataType: 'json',
            xhrFields: { withCredentials: true }
        }).done(function (data) {
            const details = parseStatus(data);
            renderCheckoutDetails(details);
        }).fail(function (xhr) {
            let err = 'Không thể kiểm tra trạng thái thanh toán.';
            if (xhr.status === 403) {
                err = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
            } else if (xhr.responseJSON && xhr.responseJSON.message) {
                err = xhr.responseJSON.message;
            } else if (xhr.responseText) {
                err = xhr.responseText;
            }
            $('#payment-status-msg').removeClass('text-muted').addClass('text-danger').text(err);
            $('#payment-status-hint').text('Thử tải lại trang hoặc liên hệ hỗ trợ.');
        });
    }

    function loadInitial() {
        pollStatus();
        pollTimer = setInterval(pollStatus, pollIntervalMs);
    }

    $('#sandbox-simulate-btn').on('click', function () {
        const $btn = $(this);
        $btn.prop('disabled', true).text('Đang xác nhận...');
        $.ajax({
            url: '/api/payment/sandbox-simulate/' + encodeURIComponent(orderId),
            type: 'POST',
            headers: csrfToken ? { 'X-CSRF-TOKEN': csrfToken } : undefined,
            dataType: 'json',
            xhrFields: { withCredentials: true }
        }).done(function (data) {
            renderCheckoutDetails(parseStatus(data));
        }).fail(function (xhr) {
            let err = 'Không thể xác nhận thử.';
            if (xhr.responseText) {
                err = xhr.responseText;
            }
            $('#payment-status-msg').addClass('text-danger').text(err);
        }).always(function () {
            $btn.prop('disabled', false).text('Đã chuyển khoản — xác nhận thử (sandbox)');
        });
    });

    loadInitial();
});
