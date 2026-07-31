/* global jQuery */
var $ = window.jQuery;

const shippingPrice = document.getElementsByClassName('shipping-price');
const discountPrice = document.getElementById('final-price');
const totalBillDisplay = document.getElementById('final-price');

var vietqrPollTimer = null;
var vietqrCountdownTimer = null;
var vietqrExpireAtMs = null;
var vietqrLocallyExpired = false;
var vietqrActiveOrderId = null;
var vietqrPaymentPageUrl = '';
var vietqrPaymentWindow = null;
var VIETQR_PAYMENT_WINDOW_NAME = 'debase_vietqr_payment';
var vietqrCanUse = false;
var vietqrPilotMode = false;
var vietqrVisibleToAll = true;
var vietqrSessionUnlocked = false;
var vietqrWrongPasswordMessage =
    'Tính năng chưa phát hành. Vui lòng đợi đến khi phát hành. Vui lòng chọn phương thức thanh toán khi nhận hàng (COD) và xác nhận lại.';
var appliedVoucherDiscountPercent = 0;
var VIETQR_EXPIRE_MESSAGE = 'Phiên thanh toán đã hết hạn. Vui lòng đặt hàng lại.';
var VIETQR_COUNTDOWN_MS = 5 * 60 * 1000;

(function ($) {
    'use strict';

    $(function () {
        if (!$ || !$.fn) {
            console.error('payment-detail: jQuery is not loaded');
            return;
        }
        initVietQrPilotGate();
        bindOrderPlacementHandlers();
        refreshVietQrAccessState();
        applyPaymentMethodRules();

        $('#vietqr-reopen-btn').on('click', function () {
            openVietQrPaymentTab(vietqrPaymentPageUrl);
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
})(window.jQuery);

function bindOrderPlacementHandlers() {
    var $form = $('#create-order');
    var $orderBtn = $('#order-btn');

    $orderBtn.on('click', function (event) {
        event.preventDefault();
        handlePlaceOrderClick();
    });

    $form.on('submit', function (event) {
        event.preventDefault();
        handlePlaceOrderClick();
    });
}

function handlePlaceOrderClick() {
    try {
        var form = document.getElementById('create-order');
        var orderBtn = document.getElementById('order-btn');
        var originalButtonText = 'Đặt Hàng';

        if (!form || !orderBtn || orderBtn.disabled) {
            return;
        }

        if (orderBtn.dataset.originalLabel) {
            originalButtonText = orderBtn.dataset.originalLabel;
        } else {
            orderBtn.dataset.originalLabel = orderBtn.textContent.trim() || originalButtonText;
            originalButtonText = orderBtn.dataset.originalLabel;
        }

        if (form.checkValidity() === false) {
            form.classList.add('was-validated');
            return;
        }

        var paymentMethod = getSelectedPaymentMethod();
        if (!paymentMethod) {
            return;
        }

        if (paymentMethod === 'COD' && !isCodAllowedForCurrentOrder()) {
            showPopup('fail', 'Đơn hàng trên 1.500.000 VND chỉ được thanh toán qua VietQR. Vui lòng chọn phương thức VietQR.');
            applyPaymentMethodRules();
            return;
        }

        if (isVietQrPaymentMethod(paymentMethod) && shouldShowVietQrPilotPopup()) {
            showVietQrPilotPopup(orderBtn, originalButtonText);
            return;
        }

        setOrderButtonLoading(orderBtn, true, originalButtonText);
        performCreateOrder(form, orderBtn, originalButtonText);
    } catch (err) {
        console.error('payment-detail: handlePlaceOrderClick failed', err);
        showPopup('fail', 'Không thể xử lý đặt hàng. Vui lòng tải lại trang.');
    }
}

function getSelectedPaymentMethod() {
    var checked = document.querySelector('input[name="radio"]:checked');
    if (checked && !checked.disabled) {
        return checked.value;
    }
    var enabled = document.querySelector('input[name="radio"]:enabled:checked');
    return enabled ? enabled.value : '';
}

function setOrderButtonLoading(orderBtn, loading, originalButtonText) {
    if (!orderBtn) {
        return;
    }
    var termsCheckbox = document.getElementById('terms-accept');
    if (loading) {
        orderBtn.disabled = true;
        orderBtn.textContent = 'Đang xử lý...';
        orderBtn.classList.add('disabled');
        return;
    }
    orderBtn.textContent = originalButtonText || 'Đặt Hàng';
    orderBtn.classList.remove('disabled');
    orderBtn.disabled = termsCheckbox ? !termsCheckbox.checked : false;
}

/**
 * visible_to_all = true  → không hiện popup pilot
 * visible_to_all = false → hiện popup nếu chưa unlock trong session
 */
function shouldShowVietQrPilotPopup() {
    if (isVietQrVisibleToAll()) {
        return false;
    }
    return !(vietqrSessionUnlocked || canUseVietQrNow());
}

function refreshVietQrAccessState() {
    if (!window.VIETQR_ACCESS || !window.VIETQR_ACCESS.enabled) {
        return;
    }
    $.ajax({
        url: '/api/payment/vietqr-access',
        type: 'GET',
        dataType: 'json',
        cache: false,
        xhrFields: { withCredentials: true }
    }).done(function (access) {
        applyVietQrAccessState(access);
    }).fail(function (xhr) {
        console.warn('payment-detail: could not refresh VietQR access state', xhr.status);
    });
}

function applyVietQrAccessState(access) {
    if (!access) {
        return;
    }
    vietqrVisibleToAll = access.visibleToAll === true;
    vietqrCanUse = access.canUseVietQr === true;
    vietqrPilotMode = access.pilotMode === true;
    vietqrSessionUnlocked = access.unlocked === true || access.canUseVietQr === true;
    if (access.pilotMessage) {
        $('#vietqr-pilot-popup-message').text(access.pilotMessage);
    }
    $('#vietqr-can-use').val(access.canUseVietQr ? 'true' : 'false');
    $('#vietqr-visible-to-all').val(access.visibleToAll ? 'true' : 'false');
}

function startVietQrCheckoutFlow(data, orderBtn, originalButtonText) {
    vietqrActiveOrderId = data.orderId || '';
    vietqrPaymentPageUrl = (data.qrLink || '').trim();
    vietqrLocallyExpired = false;
    vietqrExpireAtMs = null;

    if (vietqrPaymentPageUrl && vietqrActiveOrderId) {
        try {
            sessionStorage.setItem('vietqrPaymentUrl_' + vietqrActiveOrderId, vietqrPaymentPageUrl);
        } catch (e) { /* ignore */ }
        openVietQrPaymentTab(vietqrPaymentPageUrl);
    }

    $('#vietqr-popup-order-id').text(vietqrActiveOrderId);
    $('#vietqr-popup-amount').text(formatMoneyNumber(data.amount));
    $('#vietqr-popup-content').text(data.content || '');
    setVietQrStatusMessage('Đang chờ xác nhận thanh toán...', 'pending');

    $('#vietqr-wait-view').show();
    $('#vietqr-success-view').hide();
    document.getElementById('vietqrPaymentPopup').style.display = 'flex';

    // UI đếm ngược luôn bắt đầu đúng 05:00 (client), không lấy expiresAt server
    // (tránh lệch đồng hồ server/client).
    startVietQrCountdown(Date.now() + VIETQR_COUNTDOWN_MS);

    if ($('#vietqr-sandbox-enabled').length) {
        $('#vietqr-sandbox-btn').show();
    }
    $('#vietqr-reopen-btn').show();

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
    if (details.orderCreated === true && details.status === 'PENDING') {
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
        stopVietQrCountdown();
        closeVietQrPaymentTab();
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
        showVietQrExpiredState(details.message || VIETQR_EXPIRE_MESSAGE);
        return;
    }
    if (details.status === 'PAID_ISSUE') {
        stopVietQrPolling();
        stopVietQrCountdown();
        closeVietQrPaymentTab();
        setVietQrStatusMessage(details.message ||
            'Đã nhận thanh toán. Đơn hàng đang cần hỗ trợ xử lý tồn kho; chúng tôi sẽ liên hệ với bạn.', 'error');
        $('#vietqr-sandbox-btn').hide();
        $('#vietqr-reopen-btn').hide();
        return;
    }

    // Đã hết hạn local (00:00) — không để poll PENDING ghi đè thông báo đỏ.
    if (vietqrLocallyExpired) {
        return;
    }

    // Không sync mốc đếm ngược từ server — giữ đúng 05:00 client-side.
    // Server vẫn tự CANCELLED/hoàn kho theo expires_at của mình.

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

function startVietQrCountdown(expiresAtMs) {
    if (vietqrLocallyExpired) {
        return;
    }
    var parsed = Number(expiresAtMs);
    if (!parsed || isNaN(parsed)) {
        parsed = Date.now() + VIETQR_COUNTDOWN_MS;
    }
    // Không bao giờ cho UI đếm > 5 phút.
    var maxExpireAt = Date.now() + VIETQR_COUNTDOWN_MS;
    if (parsed > maxExpireAt) {
        parsed = maxExpireAt;
    }

    if (vietqrCountdownTimer && vietqrExpireAtMs === parsed) {
        return;
    }

    vietqrExpireAtMs = parsed;
    stopVietQrCountdown();

    var row = document.getElementById('vietqr-countdown-row');
    if (row) {
        row.classList.remove('is-urgent', 'is-expired');
        row.style.display = 'flex';
    }

    renderVietQrCountdown(Math.max(0, vietqrExpireAtMs - Date.now()));
    vietqrCountdownTimer = window.setInterval(tickVietQrCountdown, 1000);
}

function tickVietQrCountdown() {
    if (!vietqrExpireAtMs || vietqrLocallyExpired) {
        return;
    }
    var remaining = Math.max(0, Math.min(VIETQR_COUNTDOWN_MS, vietqrExpireAtMs - Date.now()));
    renderVietQrCountdown(remaining);

    var row = document.getElementById('vietqr-countdown-row');
    if (remaining <= 0) {
        showVietQrExpiredState(VIETQR_EXPIRE_MESSAGE);
        pollVietQrStatusOnce();
        return;
    }
    if (row) {
        if (remaining <= 60 * 1000) {
            row.classList.add('is-urgent');
        } else {
            row.classList.remove('is-urgent');
        }
    }
}

function renderVietQrCountdown(remainingMs) {
    var cappedMs = Math.min(Math.max(0, remainingMs), VIETQR_COUNTDOWN_MS);
    var totalSec = Math.floor(cappedMs / 1000);
    var mm = pad2(Math.floor(totalSec / 60));
    var ss = pad2(totalSec % 60);
    var mmEl = document.getElementById('vietqr-countdown-mm');
    var ssEl = document.getElementById('vietqr-countdown-ss');
    if (mmEl) {
        mmEl.textContent = mm;
    }
    if (ssEl) {
        ssEl.textContent = ss;
    }
}

function pad2(value) {
    var n = Number(value) || 0;
    return (n < 10 ? '0' : '') + n;
}

function isTouchMobileOrTablet() {
    try {
        var ua = navigator.userAgent || '';
        var isIosPhone = /iPhone|iPod/i.test(ua);
        var isIpad = /iPad/i.test(ua)
            || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
        var isAndroid = /Android/i.test(ua);
        var touch = ('ontouchstart' in window) || (navigator.maxTouchPoints > 1);
        var narrow = Math.min(window.innerWidth || 0, window.screen.width || 0) < 900;
        return isIosPhone || isIpad || isAndroid || (touch && narrow);
    } catch (e) {
        return false;
    }
}

/**
 * Desktop: popup có size. Mobile: tab thường.
 * Giữ quan hệ opener (không set opener=null) để close() được trình duyệt cho phép.
 */
function openVietQrPaymentTab(url) {
    if (!url) {
        return null;
    }

    var mobile = isTouchMobileOrTablet();
    try {
        var opened = null;
        if (mobile) {
            opened = window.open(url, VIETQR_PAYMENT_WINDOW_NAME);
        } else {
            var availW = window.screen && window.screen.availWidth ? window.screen.availWidth : 1200;
            var availH = window.screen && window.screen.availHeight ? window.screen.availHeight : 800;
            var w = Math.min(480, Math.max(360, availW - 48));
            var h = Math.min(760, Math.max(560, availH - 96));
            var left = Math.max(0, Math.floor((availW - w) / 2));
            var top = Math.max(0, Math.floor((availH - h) / 2));
            var features = 'popup=yes,width=' + w + ',height=' + h
                + ',left=' + left + ',top=' + top
                + ',scrollbars=yes,resizable=yes';
            opened = window.open(url, VIETQR_PAYMENT_WINDOW_NAME, features);
            if (!opened) {
                opened = window.open(url, VIETQR_PAYMENT_WINDOW_NAME);
            }
        }

        // Chỉ cập nhật ref khi open thành công — tránh mất handle cửa sổ cũ nếu bị chặn.
        if (opened) {
            vietqrPaymentWindow = opened;
            window.__debaseVietQrPaymentWindow = opened;
            try {
                opened.focus();
            } catch (e2) { /* ignore */ }
        } else {
            console.warn('VietQR payment window was blocked by the browser');
        }
    } catch (e) {
        console.warn('openVietQrPaymentTab failed', e);
    }
    return vietqrPaymentWindow;
}

/**
 * Đóng cửa sổ VietQR một lần, không retry.
 * Chỉ thao tác khi còn Window reference và !closed — tránh mở about:blank mới ngoài ý muốn.
 */
function closeVietQrPaymentTab() {
    var ref = vietqrPaymentWindow || window.__debaseVietQrPaymentWindow;
    vietqrPaymentWindow = null;
    window.__debaseVietQrPaymentWindow = null;

    if (!ref) {
        return;
    }

    var stillOpen = false;
    try {
        stillOpen = !ref.closed;
    } catch (e) {
        stillOpen = false;
    }
    if (!stillOpen) {
        return;
    }

    try {
        ref.close();
    } catch (e2) { /* ignore */ }

    try {
        stillOpen = !ref.closed;
    } catch (e3) {
        stillOpen = false;
    }
    // Fallback: chỉ khi cửa sổ script-opened vẫn còn mở — navigate cùng name rồi đóng.
    if (!stillOpen) {
        return;
    }
    try {
        var named = window.open('about:blank', VIETQR_PAYMENT_WINDOW_NAME);
        if (named && named !== window) {
            try {
                named.close();
            } catch (e4) { /* ignore */ }
        }
    } catch (e5) { /* ignore */ }
}

function showVietQrExpiredState(message) {
    vietqrLocallyExpired = true;
    stopVietQrPolling();
    stopVietQrCountdown();
    closeVietQrPaymentTab();
    markVietQrCountdownExpired();
    setVietQrStatusMessage(message || VIETQR_EXPIRE_MESSAGE, 'error');
    var sandboxBtn = document.getElementById('vietqr-sandbox-btn');
    var reopenBtn = document.getElementById('vietqr-reopen-btn');
    if (sandboxBtn) {
        sandboxBtn.style.display = 'none';
    }
    if (reopenBtn) {
        reopenBtn.style.display = 'none';
    }
}

function markVietQrCountdownExpired() {
    var mmEl = document.getElementById('vietqr-countdown-mm');
    var ssEl = document.getElementById('vietqr-countdown-ss');
    var row = document.getElementById('vietqr-countdown-row');
    if (mmEl) {
        mmEl.textContent = '00';
    }
    if (ssEl) {
        ssEl.textContent = '00';
    }
    if (row) {
        row.classList.remove('is-urgent');
        row.classList.add('is-expired');
    }
}

function stopVietQrCountdown() {
    if (vietqrCountdownTimer) {
        window.clearInterval(vietqrCountdownTimer);
        vietqrCountdownTimer = null;
    }
}

function closeVietQrSuccessPopup() {
    stopVietQrPolling();
    stopVietQrCountdown();
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
    function loadDistricts(provinceCode, selectedDistrictCode, selectedWardCode) {
        $('#districtSelect').empty().append('<option value="">Chọn Quận/Huyện</option>');
        $('#wardSelect').empty().append('<option value="">Chọn Phường/Xã</option>');
        if (!provinceCode) {
            return;
        }
        $.get('/districts/' + provinceCode, function(districts) {
            $.each(districts, function(index, district) {
                var selected = selectedDistrictCode && district.code === selectedDistrictCode ? ' selected' : '';
                $('#districtSelect').append(
                    '<option value="' + district.code + '"' + selected + '>' + district.name + '</option>'
                );
            });
            if (selectedDistrictCode) {
                loadWards(selectedDistrictCode, selectedWardCode);
            }
        });
    }

    function loadWards(districtCode, selectedWardCode) {
        $('#wardSelect').empty().append('<option value="">Chọn Phường/Xã</option>');
        if (!districtCode) {
            return;
        }
        $.get('/wards/' + districtCode, function(wards) {
            $.each(wards, function(index, ward) {
                var selected = selectedWardCode && ward.code === selectedWardCode ? ' selected' : '';
                $('#wardSelect').append(
                    '<option value="' + ward.code + '"' + selected + '>' + ward.name + '</option>'
                );
            });
        });
    }

    $('#provinceSelect').change(function() {
        loadDistricts($(this).val(), null, null);
    });

    $('#districtSelect').change(function() {
        loadWards($(this).val(), null);
    });

    // Prefill địa chỉ từ hồ sơ KH (province đã selected sẵn trên HTML).
    var provinceCode = $('#provinceSelect').val();
    if (provinceCode) {
        var districtCode = $('#districtSelect').attr('data-selected-district');
        var wardCode = $('#wardSelect').attr('data-selected-ward');
        loadDistricts(provinceCode, districtCode || null, wardCode || null);
    }
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

var applyBtn = document.getElementById('apply-btn');
if (applyBtn) {
    applyBtn.addEventListener('click', function () {
        var voucherCode = document.getElementById('voucher').value;
        fetch('/payment/voucher?code=' + voucherCode, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json'
            }
        })
            .then(function (response) {
                if (!response.ok) {
                    showNotification('error', 'Mã giảm giá đã hết hạn');
                    return 0;
                }
                showNotification('success', 'Sử dụng mã giảm giá thành công');
                return response.text();
            })
            .then(function (discountValue) {
                updateBill(discountValue);
            })
            .catch(function (error) {
                console.error('Lỗi:', error);
            });
    });
}

function parseMoneyInt(raw) {
    if (raw == null || raw === '') {
        return 0;
    }
    var digits = String(raw).replace(/[^\d]/g, '');
    if (!digits) {
        return 0;
    }
    var value = parseInt(digits, 10);
    return isNaN(value) ? 0 : value;
}

function getSubtotalAmount() {
    var totalBillEl = document.getElementById('totalPriceBill');
    return parseMoneyInt(totalBillEl && totalBillEl.value);
}

function setOrderGrandTotalValue(grandTotal) {
    var el = document.getElementById('order-grand-total');
    if (el) {
        el.value = String(grandTotal);
    }
}

function updateBill(discountPercent) {
    if (!discountPercent || discountPercent === 0) {
        return;
    }
    appliedVoucherDiscountPercent = parseInt(discountPercent, 10) || 0;
    var subtotal = getSubtotalAmount();
    var discountAmount = Math.floor(subtotal * appliedVoucherDiscountPercent / 100);
    var shippingFee = getShippingFeeAmount();
    var grandTotal = subtotal - discountAmount + shippingFee;

    var discountDisplay = document.getElementById('discount-price');
    if (discountDisplay) {
        discountDisplay.textContent = formatMoney(discountAmount) + ' VND';
    }
    if (totalBillDisplay) {
        totalBillDisplay.textContent = formatMoney(grandTotal) + ' VND';
    }
    setOrderGrandTotalValue(grandTotal);
    applyPaymentMethodRules();
}

function getCodMaxOrderTotal() {
    if (window.PAYMENT_RULES && window.PAYMENT_RULES.codMaxOrderTotal != null) {
        return parseMoneyInt(window.PAYMENT_RULES.codMaxOrderTotal);
    }
    var el = document.getElementById('cod-max-order-total');
    if (el && el.value) {
        return parseMoneyInt(el.value);
    }
    return 1500000;
}

function getShippingFeeAmount() {
    var el = document.getElementById('shipping-fee-amount');
    if (el && el.value !== '') {
        return parseMoneyInt(el.value);
    }
    if (shippingPrice && shippingPrice.length > 0) {
        return parseMoneyInt(shippingPrice[0].textContent);
    }
    return 0;
}

function getOrderGrandTotal() {
    var grandTotalEl = document.getElementById('order-grand-total');
    if (grandTotalEl && grandTotalEl.value !== '') {
        return parseMoneyInt(grandTotalEl.value);
    }
    var subtotal = getSubtotalAmount();
    var discountAmount = Math.floor(subtotal * (appliedVoucherDiscountPercent || 0) / 100);
    return subtotal - discountAmount + getShippingFeeAmount();
}

function isCodAllowedForCurrentOrder() {
    return getOrderGrandTotal() <= getCodMaxOrderTotal();
}

function isVietQrPaymentEnabled() {
    if (window.PAYMENT_RULES && window.PAYMENT_RULES.vietQrEnabled != null) {
        return window.PAYMENT_RULES.vietQrEnabled === true || window.PAYMENT_RULES.vietQrEnabled === 'true';
    }
    var el = document.getElementById('vietqr-payment-enabled');
    return el && el.value === 'true';
}

function applyPaymentMethodRules() {
    var codAllowed = isCodAllowedForCurrentOrder();
    var vietQrEnabled = isVietQrPaymentEnabled();
    var $codItem = $('#payment-cod-item');
    var $codInput = $('#payment-cod');
    var $vietQrInput = $('#payment-vietqr');
    var $restrictionNotice = $('#cod-restriction-notice');
    var $vietQrRequiredNotice = $('#cod-vietqr-required-notice');
    var $pilotCodBtn = $('#vietqr-pilot-popup-cancel');

    if ($codItem.length) {
        $codItem.toggleClass('payment-method--disabled', !codAllowed);
    }
    if ($codInput.length) {
        $codInput.prop('disabled', !codAllowed);
        if (!codAllowed) {
            $codInput.prop('checked', false);
        }
    }

    if (!codAllowed) {
        if ($restrictionNotice.length) {
            $restrictionNotice.show();
        }
        if (vietQrEnabled && $vietQrInput.length) {
            $vietQrInput.prop('checked', true);
        }
    } else if ($restrictionNotice.length) {
        $restrictionNotice.hide();
        if ($codInput.length && !getSelectedPaymentMethod()) {
            $codInput.prop('checked', true);
        }
    }

    var checkoutBlocked = !codAllowed && !vietQrEnabled;
    if ($vietQrRequiredNotice.length) {
        $vietQrRequiredNotice.toggle(checkoutBlocked);
    }
    if ($pilotCodBtn.length) {
        $pilotCodBtn.toggle(codAllowed);
    }

    syncOrderButtonForPaymentRules(checkoutBlocked);
}

function syncOrderButtonForPaymentRules(checkoutBlocked) {
    var orderBtn = document.getElementById('order-btn');
    var termsCheckbox = document.getElementById('terms-accept');
    if (!orderBtn) {
        return;
    }
    if (checkoutBlocked) {
        orderBtn.disabled = true;
        orderBtn.title = 'Đơn hàng trên 1.500.000 VND yêu cầu thanh toán VietQR.';
        return;
    }
    orderBtn.title = '';
    orderBtn.disabled = termsCheckbox ? !termsCheckbox.checked : false;
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

function canUseVietQrNow() {
    return vietqrCanUse === true || vietqrCanUse === 'true';
}

function isVietQrVisibleToAll() {
    return vietqrVisibleToAll === true || vietqrVisibleToAll === 'true';
}

function isVietQrPaymentMethod(paymentMethod) {
    return paymentMethod === 'VIETQR' || paymentMethod === 'TRANSFER';
}

function isVietQrPaymentSelected() {
    var checked = document.querySelector('input[name="radio"]:checked');
    return checked && isVietQrPaymentMethod(checked.value);
}

/**
 * visible_to_all = true  → không chặn
 * visible_to_all = false → chặn nếu chọn VietQR và chưa unlock session
 */
function needsVietQrPilotGate(paymentMethod) {
    if (!isVietQrPaymentMethod(paymentMethod)) {
        return false;
    }
    return shouldShowVietQrPilotPopup();
}

function getVietQrWrongPasswordMessage() {
    if (window.VIETQR_ACCESS && window.VIETQR_ACCESS.wrongPasswordMessage) {
        return window.VIETQR_ACCESS.wrongPasswordMessage;
    }
    return vietqrWrongPasswordMessage;
}

function isVietQrPilotError(message) {
    if (!message) {
        return false;
    }
    if (window.VIETQR_ACCESS && window.VIETQR_ACCESS.pilotMessage) {
        if (message.indexOf(window.VIETQR_ACCESS.pilotMessage) >= 0) {
            return true;
        }
    }
    var lower = String(message).toLowerCase();
    return lower.indexOf('pilot') >= 0
        || lower.indexOf('password') >= 0
        || lower.indexOf('mật khẩu') >= 0
        || lower.indexOf('chờ tính năng') >= 0;
}

function performCreateOrder(formElement, orderBtn, originalButtonText) {
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
    const paymentMethod = getSelectedPaymentMethod();
    if (!paymentMethod) {
        showPopup('fail', 'Vui lòng chọn phương thức thanh toán.');
        setOrderButtonLoading(orderBtn, false, originalButtonText);
        return;
    }

    const data = {
        orderId: orderId, fullName: fullName, email: email, phone: phone, paymentMethod: paymentMethod,
        province: province, district: district, ward: ward, address: address, note: note, voucher: voucher
    };

    $.ajax({
        type: 'POST',
        contentType: 'application/json',
        url: '/api/payment/create',
        data: JSON.stringify(data),
        headers: buildPaymentHeaders(),
        success: function (response) {
            if (response && response.resultType === 'QR_PAYMENT') {
                startVietQrCheckoutFlow(response, orderBtn, originalButtonText);
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
            var errMsg = formatPaymentError(xhr);
            if (isVietQrPaymentSelected() && (isVietQrPilotError(errMsg) || shouldShowVietQrPilotPopup())) {
                setOrderButtonLoading(orderBtn, false, originalButtonText);
                showVietQrPilotPopup(orderBtn, originalButtonText);
                return;
            }
            showPopup('fail', errMsg);
            setOrderButtonLoading(orderBtn, false, originalButtonText);
        }
    });

    if (formElement) {
        formElement.classList.add('was-validated');
    }
}

function showVietQrPilotPopup(orderBtn, originalButtonText) {
    $('#vietqr-pilot-popup-password').val('');
    $('#vietqr-pilot-popup-error').hide().text('');
    $('#vietqrPilotPopup').data('orderBtn', orderBtn);
    $('#vietqrPilotPopup').data('originalButtonText', originalButtonText);
    document.getElementById('vietqrPilotPopup').style.display = 'flex';
    setTimeout(function () {
        $('#vietqr-pilot-popup-password').trigger('focus');
    }, 0);
}

function closeVietQrPilotPopup() {
    document.getElementById('vietqrPilotPopup').style.display = 'none';
    $('#vietqr-pilot-popup-password').val('');
    $('#vietqr-pilot-popup-error').hide().text('');
}

function switchToCodPayment() {
    if (!isCodAllowedForCurrentOrder()) {
        closeVietQrPilotPopup();
        showPopup('fail', 'Đơn hàng trên 1.500.000 VND chỉ được thanh toán qua VietQR.');
        applyPaymentMethodRules();
        return;
    }
    closeVietQrPilotPopup();
    $('#payment-vietqr').prop('checked', false);
    $('#payment-cod').prop('checked', true);
    var $section = $('.choosePayment');
    if ($section.length) {
        $('html, body').animate({ scrollTop: $section.offset().top - 80 }, 300);
    }
}

function unlockVietQrPilotAndContinue() {
    var password = ($('#vietqr-pilot-popup-password').val() || '').trim();
    var $error = $('#vietqr-pilot-popup-error');
    $error.hide().text('');

    if (!password) {
        $error.text('Vui lòng nhập mật khẩu pilot để sử dụng VietQR.').show();
        return;
    }

    var $popup = $('#vietqrPilotPopup');
    var orderBtn = $popup.data('orderBtn');
    var originalButtonText = $popup.data('originalButtonText') || 'Đặt Hàng';
    var $confirmBtn = $('#vietqr-pilot-popup-confirm');
    $confirmBtn.prop('disabled', true).text('Đang xác nhận...');

    $.ajax({
        type: 'POST',
        contentType: 'application/json',
        url: '/api/payment/vietqr-access/unlock',
        data: JSON.stringify({ password: password }),
        headers: buildPaymentHeaders(),
        success: function (data) {
            if (data && data.canUseVietQr) {
                applyVietQrAccessState(data);
                vietqrSessionUnlocked = true;
                closeVietQrPilotPopup();
                if ($('#vietqr-info-box').length) {
                    $('#vietqr-info-box').show();
                }
                if (orderBtn) {
                    orderBtn.disabled = true;
                    orderBtn.textContent = 'Đang xử lý...';
                    orderBtn.classList.add('disabled');
                }
                performCreateOrder(document.getElementById('create-order'), orderBtn, originalButtonText);
                return;
            }
            $error.text(getVietQrWrongPasswordMessage()).show();
        },
        error: function (xhr) {
            if (xhr.status === 400) {
                $error.text(getVietQrWrongPasswordMessage()).show();
                return;
            }
            $error.text(formatPaymentError(xhr)).show();
        },
        complete: function () {
            $confirmBtn.prop('disabled', false).text('Xác nhận');
        }
    });
}

function initVietQrPilotGate() {
    var $form = $('#create-order');

    if (window.VIETQR_ACCESS) {
        applyVietQrAccessState({
            visibleToAll: window.VIETQR_ACCESS.visibleToAll === true,
            canUseVietQr: window.VIETQR_ACCESS.canUse === true,
            pilotMode: window.VIETQR_ACCESS.pilotMode === true,
            unlocked: window.VIETQR_ACCESS.unlocked === true || window.VIETQR_ACCESS.canUse === true,
            pilotMessage: window.VIETQR_ACCESS.pilotMessage
        });
        if (window.VIETQR_ACCESS.wrongPasswordMessage) {
            vietqrWrongPasswordMessage = window.VIETQR_ACCESS.wrongPasswordMessage;
        }
    } else if ($form.length) {
        vietqrVisibleToAll = $form.data('vietqr-visible-to-all') === true
            || $form.data('vietqrVisibleToAll') === true
            || $form.attr('data-vietqr-visible-to-all') === 'true';
        vietqrSessionUnlocked = $form.data('vietqr-unlocked') === true
            || $form.attr('data-vietqr-unlocked') === 'true';
        vietqrCanUse = vietqrVisibleToAll || vietqrSessionUnlocked;
    } else {
        var $visibleFlag = $('#vietqr-visible-to-all');
        if ($visibleFlag.length) {
            vietqrVisibleToAll = $visibleFlag.val() === 'true';
        }
        var $canUseFlag = $('#vietqr-can-use');
        if ($canUseFlag.length) {
            vietqrCanUse = $canUseFlag.val() === 'true';
            vietqrSessionUnlocked = vietqrCanUse && !vietqrVisibleToAll;
        }
    }

    $('#vietqr-pilot-popup-confirm').on('click', unlockVietQrPilotAndContinue);
    $('#vietqr-pilot-popup-cancel').on('click', switchToCodPayment);
    $('#vietqr-pilot-popup-password').on('keydown', function (event) {
        if (event.key === 'Enter') {
            event.preventDefault();
            unlockVietQrPilotAndContinue();
        }
    });
}
