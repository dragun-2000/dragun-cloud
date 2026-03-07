$(function () {
    'use strict';

    // --- Constants ---
    const OTP_TIMEOUT_SEC = 300;
    const PHONE_REGEX = /^(0|84|\+84)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$/;
    const OTP_REGEX = /^\d{6}$/;

    // --- State ---
    let otpTimer = null;
    let otpTimeLeft = OTP_TIMEOUT_SEC;
    let currentPhone = '';

    // --- Helpers ---
    function getCsrfHeader() {
        return { 'X-CSRF-TOKEN': $('input[name="_csrf"]').val() };
    }

    function parseJsonResponse(response) {
        if (typeof response === 'object' && response !== null) return response;
        if (typeof response === 'string' && response) {
            try {
                return JSON.parse(response);
            } catch (e) {
                return null;
            }
        }
        return {};
    }

    function getApiErrorMessage(xhr, fallback) {
        if (xhr.responseJSON && xhr.responseJSON.message) return xhr.responseJSON.message;
        if (typeof xhr.responseText === 'string' && xhr.responseText.length > 0 && xhr.responseText.length < 300) {
            return xhr.responseText;
        }
        return fallback || 'Đã xảy ra lỗi. Vui lòng thử lại.';
    }

    function showErrorModal(message) {
        $('#message-error').text(message);
        $('#register-account-error').modal('show');
    }

    function validatePhone(phone) {
        if (!phone || !phone.trim()) return false;
        const cleaned = phone.trim().replace(/[\s\-\(\)]/g, '');
        return PHONE_REGEX.test(cleaned);
    }

    function setFieldValid($el, valid) {
        $el.toggleClass('is-invalid', !valid);
    }

    // --- OTP Timer ---
    function startOtpTimer() {
        otpTimeLeft = OTP_TIMEOUT_SEC;
        clearInterval(otpTimer);
        otpTimer = setInterval(function () {
            otpTimeLeft--;
            const m = Math.floor(otpTimeLeft / 60);
            const s = otpTimeLeft % 60;
            $('#otp-timer').text(m + ':' + (s < 10 ? '0' : '') + s);
            if (otpTimeLeft <= 0) {
                clearInterval(otpTimer);
                otpTimer = null;
                $('#otp-timer').text('0:00').addClass('text-danger');
                $('#resend-otp-link').removeClass('disabled').css('cursor', 'pointer');
            }
        }, 1000);
    }

    function stopOtpTimer() {
        clearInterval(otpTimer);
        otpTimer = null;
    }

    // --- API: Send OTP ---
    function sendOtp(accountData) {
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '/SA/SA001/register/send-otp',
            data: JSON.stringify(accountData),
            headers: getCsrfHeader(),
            success: function (response) {
                const res = parseJsonResponse(response);
                if (!res || res.status !== 'success') {
                    showErrorModal(res && res.message ? res.message : 'Không thể gửi OTP');
                    return;
                }
                currentPhone = accountData.phone;
                $('#otp-phone-display').text('Số điện thoại: ' + accountData.phone);
                clearOtpBoxes();
                $('#otp-error').hide();
                $('#resend-otp-link').addClass('disabled').css('cursor', 'not-allowed');
                $('#otp-timer').removeClass('text-danger');
                startOtpTimer();
                $('#register-otp-modal').modal('show');
                setTimeout(function () {
                    $('.otp-box[data-otp-index="0"]').focus();
                }, 500);
            },
            error: function (xhr) {
                showErrorModal(getApiErrorMessage(xhr, 'Không thể gửi OTP. Vui lòng thử lại.'));
            }
        });
    }

    // --- API: Verify OTP ---
    function verifyOtp(phone, otpCode) {
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '/SA/SA001/register/verify-otp',
            data: JSON.stringify({ phone: phone, otpCode: otpCode }),
            headers: getCsrfHeader(),
            success: function (data) {
                stopOtpTimer();
                $('#register-otp-modal').modal('hide');
                var raw = typeof data === 'string' ? data : (data && data.message ? data.message : '');
                var msg = raw || 'Tài khoản của bạn đã được tạo thành công. Hãy đăng nhập để bắt đầu trải nghiệm!';
                $('#message-success').text(msg);
                $('#register-account-success').modal('show');
            },
            error: function (xhr) {
                $('.otp-box').addClass('is-invalid');
                $('#otp-error').text(getApiErrorMessage(xhr, 'Mã OTP không đúng. Vui lòng thử lại.')).show();
            }
        });
    }

    // --- API: Resend OTP ---
    function resendOtp() {
        if ($('#resend-otp-link').hasClass('disabled')) return;

        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '/SA/SA001/register/resend-otp',
            data: JSON.stringify({ phone: currentPhone }),
            headers: getCsrfHeader(),
            success: function (response) {
                const res = parseJsonResponse(response);
                if (res && res.status === 'success') {
                    clearOtpBoxes();
                    $('#otp-error').hide();
                    $('#resend-otp-link').addClass('disabled').css('cursor', 'not-allowed');
                    startOtpTimer();
                } else {
                    showErrorModal(res && res.message ? res.message : 'Không thể gửi lại OTP');
                }
            },
            error: function (xhr) {
                showErrorModal(getApiErrorMessage(xhr, 'Không thể gửi lại OTP. Vui lòng thử lại.'));
            }
        });
    }

    // --- Form: collect register data ---
    function getRegisterFormData() {
        const $form = $('#register_account_form');
        const h = $form.find('input[name="height"]').val();
        const w = $form.find('input[name="weight"]').val();
        return {
            accountId: $form.find('input[name="accountId"]').val(),
            fullName: $form.find('input[name="fullName"]').val(),
            mailAddress: $form.find('input[name="mailAddress"]').val(),
            password: $form.find('input[name="password"]').val(),
            confirmPassword: $form.find('input[name="confirmPassword"]').val(),
            phone: $form.find('input[name="phone"]').val(),
            height: h ? parseInt(h) : null,
            weight: w ? parseInt(w) : null,
            province: $form.find('select[name="province"]').val(),
            district: $form.find('select[name="district"]').val(),
            ward: $form.find('select[name="ward"]').val(),
            address: $form.find('input[name="address"]').val()
        };
    }

    // --- Form: validate register (returns true if valid) ---
    function validateRegisterForm() {
        const d = getRegisterFormData();
        let valid = true;

        setFieldValid($('#mailAddress'), !!(d.mailAddress && d.mailAddress.trim()));
        if (!d.mailAddress || !d.mailAddress.trim()) valid = false;

        setFieldValid($('#password'), !!(d.password && d.password.length >= 8));
        if (!d.password || d.password.length < 8) valid = false;

        setFieldValid($('#confirmPassword'), !!(d.confirmPassword && d.confirmPassword === d.password));
        if (!d.confirmPassword || d.confirmPassword !== d.password) valid = false;

        setFieldValid($('#fullName'), !!(d.fullName && d.fullName.trim()));
        if (!d.fullName || !d.fullName.trim()) valid = false;

        setFieldValid($('#phone'), validatePhone(d.phone));
        if (!validatePhone(d.phone)) valid = false;

        if (d.height !== null && (d.height < 120 || d.height > 250)) {
            setFieldValid($('#height'), false);
            valid = false;
        } else {
            setFieldValid($('#height'), true);
        }

        if (d.weight !== null && (d.weight < 30 || d.weight > 150)) {
            setFieldValid($('#weight'), false);
            valid = false;
        } else {
            setFieldValid($('#weight'), true);
        }

        return valid;
    }

    // --- Event: phone real-time validation ---
    $('#phone').on('input blur', function () {
        const phone = $(this).val();
        const input = this;
        if (phone && !validatePhone(phone)) {
            input.setCustomValidity('Số điện thoại không hợp lệ. Ví dụ: 0912345678');
            $(this).addClass('is-invalid');
        } else {
            input.setCustomValidity('');
            $(this).removeClass('is-invalid');
        }
    });

    // --- Event: register form submit ---
    $('#register_account_form').on('submit', function (event) {
        event.preventDefault();
        event.stopPropagation();

        if (!validateRegisterForm()) {
            this.classList.add('was-validated');
            return;
        }

        sendOtp(getRegisterFormData());
        this.classList.add('was-validated');
    });

    // --- OTP Box helpers ---
    function collectOtpValue() {
        let code = '';
        $('.otp-box').each(function () {
            code += $(this).val();
        });
        $('#otp-code').val(code);
        return code;
    }

    function clearOtpBoxes() {
        $('.otp-box').val('').removeClass('is-invalid filled');
        $('#otp-code').val('');
    }

    // --- Event: OTP individual box input ---
    $(document).on('input', '.otp-box', function () {
        const val = $(this).val().replace(/\D/g, '');
        $(this).val(val.charAt(0) || '');
        $(this).toggleClass('filled', val.length > 0);

        if (val.length > 0) {
            const idx = parseInt($(this).data('otp-index'));
            if (idx < 5) {
                $('.otp-box[data-otp-index="' + (idx + 1) + '"]').focus();
            }
        }

        $('.otp-box').removeClass('is-invalid');
        $('#otp-error').hide();

        const code = collectOtpValue();
        if (code.length === 6) {
            $('#verify-otp-btn').click();
        }
    });

    // --- Event: OTP box keydown (backspace, arrows) ---
    $(document).on('keydown', '.otp-box', function (e) {
        const idx = parseInt($(this).data('otp-index'));
        if (e.key === 'Backspace') {
            if (!$(this).val() && idx > 0) {
                const $prev = $('.otp-box[data-otp-index="' + (idx - 1) + '"]');
                $prev.val('').removeClass('filled').focus();
            } else {
                $(this).val('').removeClass('filled');
            }
            collectOtpValue();
            e.preventDefault();
        } else if (e.key === 'ArrowLeft' && idx > 0) {
            $('.otp-box[data-otp-index="' + (idx - 1) + '"]').focus();
        } else if (e.key === 'ArrowRight' && idx < 5) {
            $('.otp-box[data-otp-index="' + (idx + 1) + '"]').focus();
        }
    });

    // --- Event: OTP paste (distribute digits across boxes) ---
    $(document).on('paste', '.otp-box', function (e) {
        e.preventDefault();
        const paste = (e.originalEvent.clipboardData || window.clipboardData).getData('text').replace(/\D/g, '').slice(0, 6);
        if (!paste) return;
        $('.otp-box').each(function (i) {
            const ch = paste.charAt(i) || '';
            $(this).val(ch).toggleClass('filled', ch.length > 0);
        });
        const lastIdx = Math.min(paste.length, 6) - 1;
        $('.otp-box[data-otp-index="' + lastIdx + '"]').focus();
        const code = collectOtpValue();
        if (code.length === 6) {
            $('#verify-otp-btn').click();
        }
    });

    // --- Event: OTP box focus select ---
    $(document).on('focus', '.otp-box', function () {
        $(this).select();
    });

    // --- Event: verify OTP button ---
    $('#verify-otp-btn').on('click', function () {
        const otpCode = collectOtpValue();
        if (!otpCode || !OTP_REGEX.test(otpCode)) {
            $('.otp-box').addClass('is-invalid');
            $('#otp-error').text('Vui lòng nhập mã OTP 6 số').show();
            return;
        }
        $('.otp-box').removeClass('is-invalid');
        $('#otp-error').hide();
        $('#verify-otp-btn').prop('disabled', true).text('Đang xác thực...');
        verifyOtp(currentPhone, otpCode);
        setTimeout(function () {
            $('#verify-otp-btn').prop('disabled', false).text('Xác thực');
        }, 2000);
    });

    // --- Event: resend OTP ---
    $('#resend-otp-link').on('click', function (e) {
        e.preventDefault();
        if (!$(this).hasClass('disabled')) resendOtp();
    });

    // --- Event: OTP modal closed ---
        $('#register-otp-modal').on('hidden.bs.modal', function () {
            stopOtpTimer();
            clearOtpBoxes();
            $('#otp-error').hide();
        });

    $('#cancel-otp-btn').on('click', function () {
        stopOtpTimer();
        $('#register-otp-modal').modal('hide');
    });

    // --- Event: update account form (profile) ---
    $('#update_account_form').on('submit', function (event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.checkValidity() === false) {
            this.classList.add('was-validated');
            return;
        }
        const hVal = $(this).find('input[name="height"]').val();
        const wVal = $(this).find('input[name="weight"]').val();
        if (hVal && (parseInt(hVal) < 120 || parseInt(hVal) > 250)) {
            setFieldValid($('#height'), false);
            this.classList.add('was-validated');
            return;
        }
        if (wVal && (parseInt(wVal) < 30 || parseInt(wVal) > 150)) {
            setFieldValid($('#weight'), false);
            this.classList.add('was-validated');
            return;
        }
        const data = {
            accountId: $(this).find('input[name="accountId"]').val(),
            fullName: $(this).find('input[name="fullName"]').val(),
            mailAddress: $(this).find('input[name="mailAddress"]').val(),
            password: $(this).find('input[name="password"]').val(),
            confirmPassword: $(this).find('input[name="confirmPassword"]').val(),
            phone: $(this).find('input[name="phone"]').val(),
            height: hVal ? parseInt(hVal) : null,
            weight: wVal ? parseInt(wVal) : null,
            province: $(this).find('select[name="province"]').val(),
            district: $(this).find('select[name="district"]').val(),
            ward: $(this).find('select[name="ward"]').val(),
            address: $(this).find('input[name="address"]').val()
        };
        $.ajax({
            type: 'POST',
            contentType: 'application/json',
            url: '/SA/SA001/register',
            data: JSON.stringify(data),
            headers: getCsrfHeader(),
            success: function (msg) {
                $('#message-success').text(msg);
                $('#update-account-success').modal('show');
            },
            error: function (xhr) {
                showErrorModal(getApiErrorMessage(xhr, xhr.responseText || 'Cập nhật thất bại.'));
            }
        });
        this.classList.add('was-validated');
    });

    // --- Province / District / Ward ---
    $('#provinceSelect').on('change', function () {
        const code = $(this).val();
        $('#districtSelect').empty().append('<option value="">-- Select District --</option>');
        if (code) {
            $.get('/districts/' + code, function (districts) {
                $.each(districts, function (i, d) {
                    $('#districtSelect').append('<option value="' + d.code + '">' + d.name + '</option>');
                });
            });
        }
    });

    $('#districtSelect').on('change', function () {
        const code = $(this).val();
        $('#wardSelect').empty().append('<option value="">-- Select Ward --</option>');
        if (code) {
            $.get('/wards/' + code, function (wards) {
                $.each(wards, function (i, w) {
                    $('#wardSelect').append('<option value="' + w.code + '">' + w.name + '</option>');
                });
            });
        }
    });
});
