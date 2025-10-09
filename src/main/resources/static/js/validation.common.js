const emailRegex = '^[\\w\\-\\.\\+]+@([\\w-]+\\.)+[\\w-]{2,4}$';
const passwordUserRegex = '^(?=.*\\d)(?=.*[a-z])(?=.*[a-zA-Z]).{8,}$';
const validateMessages = {
    confirmPassword: {
        pattern: 'パスワードは8~20文字以内半角英字と半角数字と半角記号の3種類混合で入力してください。'
    },
    oldPassword: {
        pattern: 'パスワードは8~20文字以内半角英字と半角数字と半角記号の3種類混合で入力してください。'
    },
    newPassword: {
        pattern: 'パスワードは8~20文字以内半角英字と半角数字と半角記号の3種類混合で入力してください。'
    },
    fileName: {
        pattern: 'ファイル形式は正しく無いです。',
        duplicate: 'ファイル名は重複しています。'
    }
};

$(function () {
    $('[data-toggle="tooltip"]').tooltip()
});

function addPatternAttr(elementId, regex) {
    if ($('#' + elementId).length) {
        $('#' + elementId).attr('pattern', regex);
    }
}

function addPatternClassAttr(elementClass, regex) {
    if ($('.' + elementClass).length) {
        $('.' + elementClass).attr('pattern', regex);
    }
}

function checkMailRegex(elementId, value) {
    if ($('#' + elementId).length > 0) {
        const result = /^(([^<>()[\]\\.,;:\s@"]+(\.[^<>()[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/.test(value);
        return result;
    } else {
        return false;
    }
}

function checkPassRegex(elementId, value) {
    if ($('#' + elementId).length > 0) {
        const result = /^(?=.*\d)(?=.*[a-z])(?=.*[a-zA-Z]).{8,}$/.test(value);
        return result;
    } else {
        return false;
    }
}

function checkResetPassword() {
    let isMatch = true;

    const newPasswordElement = $("#newPassword");
    const newPassword = newPasswordElement.val();

    const confirmPasswordElement = $("#confirmPassword");
    const confirmPassword = confirmPasswordElement.val();

    if (newPassword === '' || typeof newPassword === 'undefined') {
        isMatch = false;
        $('.newPassword-error').html("Vui lòng nhập mật khẩu mới");
    } else if (!checkPassRegex('newPassword', newPassword)) {
        isMatch = false;
        $('.newPassword-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
    }

    if (confirmPassword === '' || typeof confirmPassword === 'undefined') {
        isMatch = false;
        $('.confirmPassword-error').html("Vui lòng nhập mật khẩu xác nhận");
    } else if (!checkPassRegex('confirmPassword', confirmPassword)) {
        isMatch = false;
        $('.confirmPassword-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
    }

    return isMatch;
}

function checkConfirmPasswordSetting() {
    let isMatch = true;
    const oldPasswordElement = $("#oldPassword");
    const oldPassword = oldPasswordElement.val();

    const newPasswordElement = $("#newPassword");
    const newPassword = newPasswordElement.val();

    const confirmPasswordElement = $("#confirmPassword");
    const confirmPassword = confirmPasswordElement.val();
    confirmPasswordElement.get(0).setCustomValidity('');

    const setting = $("#setting").val();
    if (setting === 'true') {
        if (typeof oldPassword == 'undefined' || oldPassword === '') {
            isMatch = false;
            $('.oldPassword-error').html("Yêu Cầu Nhập Mật Khẩu Hiện Tại");
        } else if (typeof oldPassword !== 'undefined' && oldPassword !== '' && !checkPassRegex('oldPassword', oldPassword)) {
            isMatch = false;
            $('.oldPassword-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
        }
    }

    if (typeof newPassword == 'undefined' || newPassword === '') {
        isMatch = false;
        $('.newPassword-error').html("Yêu Cầu Nhập Mật Khẩu Mới");
    } else if (newPassword !== '' && !checkPassRegex('newPassword', newPassword)) {
        isMatch = false;
        $('.newPassword-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
    }

    if (typeof confirmPassword == 'undefined' || confirmPassword === '') {
        isMatch = false;
        $('.confirmPassword-error').html("Yêu Cầu Xác Nhận Mật Khẩu Mới");
    } else if (confirmPassword !== '' && !checkPassRegex('confirmPassword', confirmPassword)) {
        isMatch = false;
        $('.confirmPassword-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
    }

    return isMatch;
}

function checkEmailValid() {
    let isValid = true;
    const mailAddressElement = $("#email");
    const mailAddress = mailAddressElement.val();

    if (typeof mailAddress == 'undefined' || mailAddress === '') {
        isValid = false;
        $('.mailAddress-error').html("Vui lòng nhập địa chỉ email");
        $('.mailAddress-error').css('font-size', '12px');
        $('.mailAddress-error').css('color', 'red');
    } else if (!checkMailRegex('email', mailAddress)) {
        isValid = false;
        $('.mailAddress-error').html("Địa chỉ email không hợp lệ");
        $('.mailAddress-error').css('font-size', '12px');
        $('.mailAddress-error').css('color', 'red');
    } else {
        $('.mailAddress-error').html("");
    }
    return isValid;
}

function checkFormRegistrationAccount() {
    let isValid = true;
    const mailAddressElement = $("#mailAddress");
    const mailAddress = mailAddressElement.val();

    const passwordElement = $("#password");
    const password = passwordElement.val();

    const confirmPasswordElement = $("#confirmPassword");
    const confirmPassword = confirmPasswordElement.val();
    confirmPasswordElement.get(0).setCustomValidity('');

    if (typeof mailAddress == 'undefined' || mailAddress == '') {
        isValid = false;
        $('.mailAddress-error').html("Vui lòng nhập địa chỉ email");
    } else if (!checkMailRegex('mailAddress', mailAddress)) {
        isValid = false;
        $('.mailAddress-error').html("Địa chỉ email không hợp lệ.");
    }

    if (typeof password == 'undefined' || password == '') {
        isValid = false;
        $('.password-error').html("Vui lòng nhập mật khẩu");
    } else if (!checkPassRegex('password', password)) {
        isValid = false;
        $('.password-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
    }

    if (typeof confirmPassword == 'undefined' || confirmPassword == '') {
        isValid = false;
        $('.confirmPassword-error').html("Vui lòng nhập mật khẩu xác nhận của bạn");
    } else if (!checkPassRegex('confirmPassword', confirmPassword)) {
        isValid = false;
        $('.confirmPassword-error').html("Vui lòng nhập mật khẩu của bạn trong vòng 8 đến 20 ký tự bao gồm chữ và số");
    }

    return isValid;
}