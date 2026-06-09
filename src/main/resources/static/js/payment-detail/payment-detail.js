const shippingPrice = document.getElementsByClassName('shipping-price');
const totalBill = document.getElementById('totalPriceBill');
const discountPrice = document.getElementById('final-price');
const totalBillDisplay = document.getElementById('final-price');

var vietqrPollTimer = null;
var vietqrActiveOrderId = null;
var vietqrPaymentPageUrl = '';

$(function () {
    'use strict';

    $(function () {
        $("#create-order").on('submit', function (event) {
            const orderBtn = document.getElementById('order-btn');
            const originalButtonText = orderBtn ? orderBtn.textContent : 'Đặt Hàng';

            if (orderBtn && orderBtn.disabled) {
                event.preventDefault();
                event.stopPropagation();
                return false;
            }

            if (orderBtn) {
                orderBtn.disabled = true;
                orderBtn.textContent = 'Đang xử lý...';
                orderBtn.classList.add('disabled');
            }

            if (this.checkValidity() === false) {
                event.preventDefault();
                event.stopPropagation();
                if (orderBtn) {
                    orderBtn.disabled = false;
                    orderBtn.textContent = originalButtonText;
                    orderBtn.classList.remove('disabled');
                }
            } else {
                const orderId = $('input[name=orderId]').val();
                const fullName = $('input[name=fullName]').val();
                const email = $('input[name=email]').val();
                const phone = $('input[name=phone]').val();
                const province = $('select[name=province]').val();
                const district = $('select[name=district]').val();
                const ward = $('select[name=ward]').val();
                const address = $('input[name=address]').val();
                const note = $('textarea[name=note]').val();
                const voucher = $('input[name=voucher]').val();
                const paymentMethod = document.querySelector('input[name="radio"]:checked').value;

                const data = {
                    orderId: orderId, fullName: fullName, email: email, phone: phone, paymentMethod: paymentMethod,
                    province: province, district: district, ward: ward, address: address, note: note, voucher: voucher
                };
                $.ajax({
                    type: "POST",
                    contentType: "application/json",
                    url: "/api/payment/create",
                    data: JSON.stringify(data),
                    headers: buildPaymentHeaders(),
                    success: function (data) {
                        if (data && data.resultType === 'QR_PAYMENT') {
                            startVietQrCheckoutFlow(data, orderBtn, originalButtonText);
                            return;
                        }
                        if (typeof updateHeaderBagCount === 'function') {
                            updateHeaderBagCount(0);
                        }
                        if (orderBtn) {
                            orderBtn.disabled = false;
                            orderBtn.textContent = originalButtonText;
                            orderBtn.classList.remove('disabled');
                        }
                        showPopup('success');
                    },
                    error: function (xhr) {
                        console.error('payment/create failed', xhr.status, xhr.responseJSON || xhr.responseText);
                        showPopup('fail', formatPaymentError(xhr));
                        if (orderBtn) {
                            orderBtn.disabled = false;
                            orderBtn.textContent = originalButtonText;
                            orderBtn.classList.remove('disabled');
                        }
                    }
                });
                event.preventDefault();
            }
            this.classList.add('was-validated');
        });
    });

    $('#vietqr-reopen-btn').on('click', function () {
        if (vietqrPaymentPageUrl) {
            window.open(vietqrPaymentPageUrl, '_blank', 'noopener,noreferrer');
        }
    });

    $('#vietqr-sandbox-btn').on('click', function () {
        if (!vietqrActiveOrderId) {
            return;
        }
        var $btn = $(this);
        $btn.prop('disabled', true);
        $.ajax({
            url: '/api/payment/sandbox-simulate/' + encodeURIComponent(vietqrActiveOrderId),
            type: 'POST',
            dataType: 'json',
            xhrFields: { withCredentials: true }
        }).done(function (data) {
            handleVietQrStatus(data);
        }).fail(function (xhr) {
            var err = 'Không thể xác nhận thử.';
            if (xhr.responseJSON && xhr.responseJSON.message) {
                err = xhr.responseJSON.message;
            }
            setVietQrStatusMessage(err, 'error');
            pollVietQrStatusOnce();
        }).always(function () {
            $btn.prop('disabled', false);
        });
    });
});

function startVietQrCheckoutFlow(data, orderBtn, originalButtonText) {
    vietqrActiveOrderId = data.orderId || '';
    vietqrPaymentPageUrl = (data.qrLink || '').trim();

    if (vietqrPaymentPageUrl && vietqrActiveOrderId) {
        try {
            sessionStorage.setItem('vietqrPaymentUrl_' + vietqrActiveOrderId, vietqrPaymentPageUrl);
        } catch (e) { /* ignore */ }
        window.open(vietqrPaymentPageUrl, '_blank', 'noopener,noreferrer');
    }

    $('#vietqr-popup-order-id').text(vietqrActiveOrderId);
    $('#vietqr-popup-amount').text(formatMoneyNumber(data.amount));
    $('#vietqr-popup-content').text(data.content || '');
    setVietQrStatusMessage('Đang chờ xác nhận thanh toán...', 'pending');

    $('#vietqr-wait-view').show();
    $('#vietqr-success-view').hide();
    document.getElementById('vietqrPaymentPopup').style.display = 'flex';

    if ($('#vietqr-sandbox-enabled').length) {
        $('#vietqr-sandbox-btn').show();
    }

    if (orderBtn) {
        orderBtn.textContent = 'Đang chờ thanh toán...';
    }

    pollVietQrStatusOnce();
    stopVietQrPolling();
    vietqrPollTimer = setInterval(pollVietQrStatusOnce, 3000);
}

document.addEventListener('visibilitychange', function () {
    if (document.visibilityState === 'visible' && vietqrActiveOrderId && vietqrPollTimer) {
        pollVietQrStatusOnce();
    }
});

function pollVietQrStatusOnce() {
    if (!vietqrActiveOrderId) {
        return;
    }
    $.ajax({
        url: '/api/payment/status/' + encodeURIComponent(vietqrActiveOrderId),
        type: 'GET',
        dataType: 'json',
        cache: false,
        xhrFields: { withCredentials: true }
    }).done(function (data) {
        if (!data || typeof data !== 'object') {
            return;
        }
        if (!data.status && data.result && data.message) {
            setVietQrStatusMessage(data.message, 'error');
            return;
        }
        handleVietQrStatus(data);
    }).fail(function (xhr) {
        var err = 'Không thể kiểm tra trạng thái thanh toán.';
        if (xhr.status === 403) {
            err = 'Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.';
        } else if (xhr.responseJSON && xhr.responseJSON.message) {
            err = xhr.responseJSON.message;
        }
        setVietQrStatusMessage(err, 'error');
    });
}

function normalizeVietQrPaymentStatus(status) {
    if (status == null || status === '') {
        return '';
    }
    return String(status).trim().toUpperCase();
}

function handleVietQrStatus(data) {
    var details = typeof data === 'string' ? { status: data } : (data || {});
    details.status = normalizeVietQrPaymentStatus(details.status);
    if (details.orderCreated === true && details.status !== 'EXPIRED') {
        details.status = 'PAID';
    }
    if (details.qrLink) {
        vietqrPaymentPageUrl = details.qrLink;
    }
    if (details.amount != null) {
        $('#vietqr-popup-amount').text(formatMoneyNumber(details.amount));
    }
    if (details.content) {
        $('#vietqr-popup-content').text(details.content);
    }

    if (details.status === 'PAID') {
        stopVietQrPolling();
        try {
            sessionStorage.removeItem('vietqrPaymentUrl_' + vietqrActiveOrderId);
        } catch (e) { /* ignore */ }
        if (typeof updateHeaderBagCount === 'function') {
            updateHeaderBagCount(0);
        }
        showVietQrSuccessPopup(details.message);
        return;
    }
    if (details.status === 'EXPIRED') {
        stopVietQrPolling();
        setVietQrStatusMessage(details.message || 'Phiên thanh toán đã hết hạn. Vui lòng đặt hàng lại.', 'error');
        $('#vietqr-sandbox-btn').hide();
        return;
    }

    var waitMsg = details.message;
    if (!waitMsg) {
        waitMsg = details.awaitingPayment === true
            ? 'Đã tạo mã QR — chưa xác nhận chuyển khoản. Hoàn tất thanh toán trên tab VietQR; trang sẽ tự cập nhật.'
            : 'Vui lòng hoàn tất thanh toán trên tab VietQR. Trang sẽ tự cập nhật.';
    }
    setVietQrStatusMessage(waitMsg, 'pending');
}

function setVietQrStatusMessage(text, type) {
    var $el = $('#vietqr-popup-status');
    $el.text(text);
    $el.removeClass('is-success is-error');
    if (type === 'success') {
        $el.addClass('is-success');
    } else if (type === 'error') {
        $el.addClass('is-error');
    }
}

function showVietQrSuccessPopup(message) {
    $('#vietqr-wait-view').hide();
    $('#vietqr-success-view').show();
    if (message) {
        $('#vietqr-success-message').text(message);
    }
}

function stopVietQrPolling() {
    if (vietqrPollTimer) {
        clearInterval(vietqrPollTimer);
        vietqrPollTimer = null;
    }
}

function closeVietQrSuccessPopup() {
    stopVietQrPolling();
    document.getElementById('vietqrPaymentPopup').style.display = 'none';
    window.location.href = '/order-history';
}

function formatMoneyNumber(amount) {
    if (amount === null || amount === undefined || amount === '') {
        return '';
    }
    var num = Number(amount);
    if (isNaN(num)) {
        return String(amount);
    }
    return num.toLocaleString('vi-VN');
}

$(document).ready(function() {
    $('#provinceSelect').change(function() {
      const provinceCode = $(this).val();
      $('#districtSelect').empty().append('<option value="">Chọn Quận/Huyện</option>');
        if (provinceCode) {
            $.get('/districts/' + provinceCode, function(districts) {
                $.each(districts, function(index, district) {
                    $('#districtSelect').append('<option value="' + district.code + '">' + district.name + '</option>');
                });
            });
        }
    });

    $('#districtSelect').change(function() {
      const districtCode = $(this).val();
      $('#wardSelect').empty().append('<option value="">Chọn Phường/Xã</option>');
        if (districtCode) {
            $.get('/wards/' + districtCode, function(districts) {
                $.each(districts, function(index, district) {
                    $('#wardSelect').append('<option value="' + district.code + '">' + district.name + '</option>');
                });
            });
        }
    });
});

function showPopup(type, message) {
    if (type === 'success') {
        document.getElementById("orderSuccessPopup").style.display = "flex";
    } else if (type === 'fail') {
        setErrorMessage(message)
        document.getElementById("orderFailedPopup").style.display = "flex";
    }
}

function buildPaymentHeaders() {
    const csrf = $("input[name='_csrf']").val();
    if (csrf) {
        return { 'X-CSRF-TOKEN': csrf };
    }
    return {};
}

function formatPaymentError(xhr) {
    if (xhr.responseJSON && xhr.responseJSON.message) {
        return xhr.responseJSON.message;
    }
    if (xhr.responseText) {
        try {
            const parsed = JSON.parse(xhr.responseText);
            if (parsed.message) {
                return parsed.message;
            }
        } catch (e) {
            return xhr.responseText;
        }
    }
    return 'Lỗi ' + xhr.status + ': Không thể tạo đơn hàng';
}

function setErrorMessage(message) {
    const errorMessageElement = document.querySelector('#orderFailedPopup .error-message');
    if (errorMessageElement) {
        errorMessageElement.textContent = message;
    }
}

function closePopup(popupId) {
    document.getElementById(popupId).style.display = "none";
    if (popupId === 'orderSuccessPopup') {
        window.location.href = '/order-history';
    } else {
        window.location.href = '/cart-detail';
    }
}

document.getElementById('apply-btn').addEventListener('click', function() {
    let voucherCode = document.getElementById('voucher').value;
    fetch('/payment/voucher?code=' + voucherCode, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json'
        },
    })
    .then(response => {
        if (!response.ok) {
            showNotification('error', 'Mã giảm giá đã hết hạn')
            return 0;
        } else {
            showNotification('success', 'Sử dụng mã giảm giá thành công')
            return response.text();
        }
    })
    .then(discountPrice => {
        updateBill(discountPrice);
    })
    .catch((error) => {
        console.error('Lỗi:', error);
    });
});

function updateBill(discount) {
    if (discount === 0) return;
    let tempPrice = totalBill.value;
    discountPrice.textContent = formatMoney(parseInt(discount)) + ' VND';
    totalBillDisplay.textContent = formatMoney(parseInt(tempPrice) - parseInt(discount) + 35000) + ' VND';
}

function formatMoney(value) {
    return value.toLocaleString('vi-VN');
}

function showNotification(type, message) {
    const notification = document.getElementById('notification');
    const notificationMessage = document.getElementById('notification-message');

    notificationMessage.textContent = message;
    notification.className = `notification ${type} show`;

    setTimeout(() => {
        hideNotification();
    }, 2000);
}

function hideNotification() {
    const notification = document.getElementById('notification');
    if (notification) {
        notification.classList.remove('show');
    }
}

const modal = document.getElementById("imageModal");
function closeModal() {
    if (typeof modal !== 'undefined' && modal) {
        modal.classList.remove("show");
    }
}
