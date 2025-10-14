$(function () {
    'use strict';

    $("#update_account_form").on('submit', function (event) {
        const fullName = $('input[name=fullName]').val();
        const mailAddress = $('input[name=mailAddress]').val();
        const phone = $('input[name=phone]').val();
        event.preventDefault();

        if (fullName.length > 0 && mailAddress.length > 0 && checkMailRegex('mailAddress', mailAddress)) {
            $('#modal-approve-master').modal('show');
        }
    });

    $("#submit-redirect-list").attr('href', '/AM/AM004-02');

    $("#submit-update-account").on('click', function (event) {
        const fullName = $('input[name=fullName]').val();
        const mailAddress = $('input[name=mailAddress]').val();
        const phone = $('input[name=phone]').val();
        const accountId = $('input[name=accountId]').val();

        var data = {accountId: accountId, fullName: fullName, mailAddress: mailAddress, phone: phone};
        if (typeof fullName !== 'undefined' && fullName !== '') {
            $.ajax({
                type: "POST",
                contentType: "application/json",
                url: "/AM/AM004/update",
                data: JSON.stringify(data),
                headers: {"X-CSRF-TOKEN": $("input[name='_csrf']").val()},
                success: function (data) {
                    $('#update-account-success').modal('show');
                },
                error: function(error) {
                    console.log('error', error);
                    $('#message-error').text(error.responseText);
                    $('#update-account-error').modal('show');
                }
            })
        }
    });
});

window.addEventListener('load', function (ev) {
    addPatternAttr('mailAddress', emailRegex);
});
