$(function () {
    'use strict';

    $("#form-reset-password").on('submit', function (event) {
        var self = this;
        var isMatch = checkResetPassword();
        if (this.checkValidity() === false || !isMatch) {
            event.preventDefault();
            event.stopPropagation();
        } else {
            var accountId = $('input[name=accountId]').val();
            var hash = $('input[name=hash]').val();
            var newPassword = $('input[name=newPassword]').val();
            var confirmPassword = $('input[name=confirmPassword]').val();
            var data = {accountId: accountId, hash: hash, newPassword: newPassword, confirmPassword: confirmPassword};
            $.ajax({
                type: 'POST',
                contentType: "application/json",
                data: JSON.stringify(data),
                url: '/AM/AM001/reset_password',
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

