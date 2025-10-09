$(function () {
    'use strict';
    
    $("#form-reset-password-sa").on('submit', function (event) {
        const self = this;
        const isMatch = checkResetPassword();
        if (this.checkValidity() === false || !isMatch) {
            event.preventDefault();
            event.stopPropagation();
        } else {
            const accountId = $('input[name=accountId]').val();
            const hash = $('input[name=hash]').val();
            const newPassword = $('input[name=newPassword]').val();
            const confirmPassword = $('input[name=confirmPassword]').val();
            const data = {accountId: accountId, hash: hash, newPassword: newPassword, confirmPassword: confirmPassword};
            console.log('form-reset-password-sa');
            $.ajax({
                type: 'POST',
                contentType: "application/json",
                data: JSON.stringify(data),
                url: '/SA/SA002/reset_password',
                headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                success: function (data) {
                    $('#reset-password-success').modal('show');
                    $('#reset-password-error').modal('hide');
                },
                error: function (error) {
                    $('#message-error').html(error.responseText);
                    $('#reset-password-error').modal('show');
                    $('#reset-password-success').modal('hide');
                }
            });
            event.preventDefault();
        }
        this.classList.add('was-validated');
    });
});

window.addEventListener('load', function (ev) {
    addPatternAttr('newPassword', passwordUserRegex);
    addPatternAttr('confirmPassword', passwordUserRegex);
});

