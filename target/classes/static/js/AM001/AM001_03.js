$(function () {
    'use strict';

    $(function () {
        $("#form-change-password").on('submit', function (event) {
            var self = this;
            var isMatch = checkConfirmPasswordSetting();
            if (this.checkValidity() === false || !isMatch) {
                event.preventDefault();
                event.stopPropagation();
            } else {
                var accountId = $('input[name=accountId]').val();
                var oldPassword = $('input[name=oldPassword]').val();
                var newPassword = $('input[name=newPassword]').val();
                var confirmPassword = $('input[name=confirmPassword]').val();
                var data = {accountId: accountId, oldPassword: oldPassword, newPassword: newPassword, confirmPassword: confirmPassword};
                $.ajax({
                    type: 'POST',
                    contentType: "application/json",
                    data: JSON.stringify(data),
                    url: '/AM/AM001/change-password',
                    headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                    success: function (data) {
                        $('#change-password-success').modal('show');
                        $('#change-password-error').modal('hide');
                    },
                    error: function (error) {
                        $('#message-error').html(error.responseText);
                        $('#change-password-error').modal('show');
                        $('#change-password-success').modal('hide');
                    }
                });
                event.preventDefault();
            }
            this.classList.add('was-validated');
        });
    });
});

window.addEventListener('load', function (ev) {
    addPatternAttr('newPassword', passwordUserRegex);
    addPatternAttr('oldPassword', passwordUserRegex);
    addPatternAttr('confirmPassword', passwordUserRegex);
});

