$(function () {
    'use strict';

    $("#forget-password").on('submit', function (event) {
        var self = this;
        var isMatch = checkEmailValid();
        if (this.checkValidity() === false || !isMatch) {
            event.preventDefault();
            event.stopPropagation();
        } else {
            var email = $('input[name=email]').val();
            var data = {email: email};
            $.ajax({
                type: 'POST',
                contentType: "application/json",
                data: JSON.stringify(data),
                url: '/AM/AM001/send-mail',
                headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                success: function (data) {
                    $('#forget-password-success').modal('show');
                    $('#forget-password-success').modal('hide');
                },
                error: function (error) {
                    $('#message-error').html(error.responseText);
                    $('#forget-password-error').modal('show');
                    $('#forget-password-error').modal('hide');
                }
            });
            event.preventDefault();
        }
        this.classList.add('was-validated');
    });
    
    $("#forget-password-sa").on('submit', function (event) {
        const self = this;
        const isMatch = checkEmailValid();
        if (this.checkValidity() === false || !isMatch) {
            event.preventDefault();
            event.stopPropagation();
        } else {
            const email = $('input[name=email]').val();
            const data = {email: email};
            $.ajax({
                type: 'POST',
                contentType: "application/json",
                data: JSON.stringify(data),
                url: '/SA/SA002/send-mail',
                headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                success: function (data) {
                    $('#forget-password-success').modal('show');
                    $('#forget-password-success').modal('hide');
                },
                error: function (error) {
                    $('#message-error').html(error.responseText);
                    $('#forget-password-error').modal('show');
                    $('#forget-password-error').modal('hide');
                }
            });
            event.preventDefault();
        }
        this.classList.add('was-validated');
    });
});

window.addEventListener('load', function (ev) {
    addPatternAttr('email', emailRegex);
    const emailElement = $('#email');

    emailElement.on('input focus', function (event) {
        if (emailElement.val() !== $(this).val() && $(this).val()) {
            emailElement.get(0).setCustomValidity(validateMessages.email.pattern);
        } else {
            emailElement.get(0).setCustomValidity('');
        }
    });
});

